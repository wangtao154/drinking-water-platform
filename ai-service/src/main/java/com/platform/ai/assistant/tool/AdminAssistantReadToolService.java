package com.platform.ai.assistant.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.ai.assistant.dto.AdminAssistantDataSourceVO;
import com.platform.ai.config.AdminAssistantToolProperties;
import com.platform.common.auth.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Controlled data access for the admin assistant.
 *
 * <p>Every request is a fixed, internal GET endpoint selected by code. The
 * model never receives database credentials, a request URL, raw page rows, or
 * any ability to create a write request.</p>
 *
 * <p>Since 2026-08-21 tool selection and parameter extraction (device id,
 * time range, detail level) are performed by the model through function
 * calling; regular expressions are used only to validate the shape of the
 * returned parameters, never to understand the question.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAssistantReadToolService {

    private static final DateTimeFormatter QUERY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    /** Safety validation only — the model extracts the identifier, this checks its shape. */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_-]{2,20}$");

    private final AdminAssistantToolProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    /**
     * Validates one model-returned tool call: the name must be whitelisted and
     * the arguments must pass shape checks. Unknown or malformed calls are
     * dropped here before any internal request is made.
     */
    public Optional<AssistantToolSchemas.ToolInvocation> parseInvocation(String toolName, String argumentsJson) {
        if (resolveTool(toolName) == null) {
            log.warn("[AssistantTool] Rejected unknown tool name from model: {}", toolName);
            return Optional.empty();
        }
        try {
            JsonNode arguments = objectMapper.readTree(
                    StringUtils.hasText(argumentsJson) ? argumentsJson : "{}");
            String deviceId = arguments.path("device_id").asText("").trim();
            if (StringUtils.hasText(deviceId) && !SAFE_IDENTIFIER.matcher(deviceId).matches()) {
                log.warn("[AssistantTool] Rejected malformed device_id from model, tool={}", toolName);
                return Optional.empty();
            }
            LocalDateTime start = parseIsoTime(arguments.path("start_time"));
            LocalDateTime end = parseIsoTime(arguments.path("end_time"));
            if (start != null && end != null && !end.isAfter(start)) {
                start = null;
                end = null;
            }
            boolean detail = arguments.path("detail").asBoolean(false);
            return Optional.of(new AssistantToolSchemas.ToolInvocation(toolName,
                    deviceId.isEmpty() ? null : deviceId, start, end, detail));
        } catch (Exception ex) {
            log.warn("[AssistantTool] Failed to parse tool arguments: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public List<AssistantToolResult> collect(List<AssistantToolSchemas.ToolInvocation> invocations, CurrentUser user) {
        if (!properties.isEnabled() || invocations == null || invocations.isEmpty()) {
            return List.of();
        }
        List<AssistantToolResult> results = new ArrayList<>();
        for (AssistantToolSchemas.ToolInvocation invocation : invocations) {
            AssistantReadTool tool = resolveTool(invocation.toolName());
            if (tool == null) {
                log.warn("[AssistantTool] Skipped unknown tool: {}", invocation.toolName());
                continue;
            }
            results.add(execute(tool, invocation, user));
        }
        return results;
    }

    private AssistantReadTool resolveTool(String toolName) {
        return switch (toolName == null ? "" : toolName) {
            case "device_snapshot" -> AssistantReadTool.DEVICE;
            case "device_production" -> AssistantReadTool.DEVICE_PRODUCTION;
            case "alerts" -> AssistantReadTool.ALERTS;
            case "work_orders" -> AssistantReadTool.WORK_ORDERS;
            case "scan_orders" -> AssistantReadTool.SCAN_ORDERS;
            case "dashboard" -> AssistantReadTool.DASHBOARD;
            case "order_report" -> AssistantReadTool.ORDER_REPORT;
            default -> null;
        };
    }

    private AssistantToolResult execute(AssistantReadTool tool, AssistantToolSchemas.ToolInvocation invocation,
                                        CurrentUser user) {
        if (!user.hasPermission(tool.permission())) {
            return result(tool, "DENIED", "当前账号没有此数据的查看权限", "无", Map.of(),
                    "当前用户无权访问该数据。不得推断或补充任何相关业务数据。");
        }
        try {
            return switch (tool) {
                case DEVICE -> deviceSnapshot(invocation);
                case DEVICE_PRODUCTION -> deviceProductionStats(invocation);
                case ALERTS -> alertStats(invocation);
                case WORK_ORDERS -> workOrderStats(invocation);
                case SCAN_ORDERS -> scanOrderStats(invocation);
                case DASHBOARD -> dashboardStats(invocation);
                case ORDER_REPORT -> orderReportStats(invocation);
            };
        } catch (Exception ex) {
            log.warn("[AssistantTool] Read failed, userId={}, tool={}, error={}",
                    user.getUserId(), tool.name(), ex.getClass().getSimpleName());
            return result(tool, "UNAVAILABLE", "暂时无法取得该数据，请稍后在对应业务页面核对", "当前查询失败", Map.of(),
                    "该受控数据暂时不可用，不得将其当作正常或异常数据进行推断。");
        }
    }

    private AssistantToolResult deviceSnapshot(AssistantToolSchemas.ToolInvocation invocation)
            throws IOException, InterruptedException {
        QueryTimeRange timeRange = toTimeRange(invocation);
        if (StringUtils.hasText(invocation.deviceId())) {
            JsonNode record = getInternalData(properties.getDeviceBaseUrl() + "/api/v1/devices/internal/assistant/device?identifier="
                    + URLEncoder.encode(invocation.deviceId(), StandardCharsets.UTF_8));
            if (!record.path("found").asBoolean(false)) {
                return result(AssistantReadTool.DEVICE, "NOT_FOUND", "未找到匹配设备", "当前设备查询",
                        Map.of("查询标识", invocation.deviceId()),
                        "未找到匹配设备，不能据此判断设备状态或历史数据。");
            }
            Map<String, Object> primaryFacts = new LinkedHashMap<>();
            primaryFacts.put("设备ID", text(record, "deviceId"));
            primaryFacts.put("型号", text(record, "modelName"));
            primaryFacts.put("在线状态", record.path("onlineStatus").asInt() == 1 ? "在线" : "离线");
            Map<String, Object> secondaryFacts = new LinkedHashMap<>();
            secondaryFacts.put("生命周期状态", text(record, "lifecycleStatus"));
            secondaryFacts.put("激活时间", text(record, "activatedAt"));
            String range = timeRange.isAllHistory() ? "当前设备状态" : "当前设备状态（在线状态不支持历史回溯）";
            return result(AssistantReadTool.DEVICE, "OK", "已取得单台设备的受控状态概况", range,
                    selectFacts(invocation.detail(), primaryFacts, secondaryFacts), null);
        }

        JsonNode data = getInternalData(withTimeRange(
                properties.getReportBaseUrl() + "/api/v1/reports/internal/assistant/devices", timeRange));
        Map<String, Object> primaryFacts = new LinkedHashMap<>();
        primaryFacts.put("当前设备总数", number(data, "totalDevices"));
        primaryFacts.put("当前在线设备", number(data, "onlineCount"));
        primaryFacts.put("当前离线设备", number(data, "offlineCount"));
        Map<String, Object> secondaryFacts = new LinkedHashMap<>();
        secondaryFacts.put("当前故障设备", number(data, "faultCount"));
        secondaryFacts.put(timeRange.isAllHistory() ? "今日登记" : "统计范围内登记", number(data, "registeredToday"));
        secondaryFacts.put(timeRange.isAllHistory() ? "今日激活" : "统计范围内激活", number(data, "activatedToday"));
        return result(AssistantReadTool.DEVICE, "OK", "已取得平台设备统计概况", timeRange.description(),
                selectFacts(invocation.detail(), primaryFacts, secondaryFacts), null);
    }

    private AssistantToolResult deviceProductionStats(AssistantToolSchemas.ToolInvocation invocation)
            throws IOException, InterruptedException {
        if (!StringUtils.hasText(invocation.deviceId())) {
            return result(AssistantReadTool.DEVICE_PRODUCTION, "NOT_FOUND", "请提供设备ID或控制板SN", "当前设备查询", Map.of(),
                    "未提供可识别的设备ID或控制板SN，不能查询设备制水历史数据。");
        }

        JsonNode device = getInternalData(properties.getDeviceBaseUrl() + "/api/v1/devices/internal/assistant/device?identifier="
                + URLEncoder.encode(invocation.deviceId(), StandardCharsets.UTF_8));
        if (!device.path("found").asBoolean(false)) {
            return result(AssistantReadTool.DEVICE_PRODUCTION, "NOT_FOUND", "未找到匹配设备", "当前设备查询",
                    Map.of("查询标识", invocation.deviceId()), "未找到匹配设备，不能据此判断制水数据。");
        }

        QueryTimeRange timeRange = productionTimeRange(toTimeRange(invocation));
        String url = properties.getIotBaseUrl() + "/api/v1/iot/internal/assets/"
                + URLEncoder.encode(text(device, "deviceId"), StandardCharsets.UTF_8) + "/production-summary"
                + "?startTime=" + URLEncoder.encode(timeRange.startTime().toString(), StandardCharsets.UTF_8)
                + "&endTime=" + URLEncoder.encode(timeRange.endTime().toString(), StandardCharsets.UTF_8);
        JsonNode data = getInternalData(url);

        Map<String, Object> primaryFacts = new LinkedHashMap<>();
        primaryFacts.put("设备ID", text(device, "deviceId"));
        primaryFacts.put("统计时段", timeRange.description());
        primaryFacts.put("当前制水状态", data.path("currentProducing").asBoolean(false) ? "正在制水" : "当前未制水");
        primaryFacts.put("制水时段数", number(data, "productionSessions"));
        primaryFacts.put("累计制水时长(分钟)", decimal(data, "productionDurationMinutes", 2));
        primaryFacts.put("纯水产量(升)", decimal(data, "pureWaterLiters", 3));

        if (!"OK".equals(data.path("dataStatus").asText())) {
            primaryFacts.put("数据状态", "NO_DATA".equals(data.path("dataStatus").asText())
                    ? "该时段没有遥测数据" : "该时段未检测到制水状态开启");
        }

        Map<String, Object> secondaryFacts = new LinkedHashMap<>();
        secondaryFacts.put("废水产量(升)", decimal(data, "wasteWaterLiters", 3));
        secondaryFacts.put("废水比", nullableDecimal(data, "wasteWaterRatio", 3));
        secondaryFacts.put("制水样本数", number(data, "productionSamples"));
        secondaryFacts.put("原水TDS均值(PPM)", nullableDecimal(data, "averageRawTds", 2));
        secondaryFacts.put("纯水TDS均值(PPM)", nullableDecimal(data, "averagePureTds", 2));
        secondaryFacts.put("膜前压力均值(bar)", nullableDecimal(data, "averageMembraneFrontPressure", 2));
        secondaryFacts.put("膜后压力均值(bar)", nullableDecimal(data, "averageMembraneRearPressure", 2));
        secondaryFacts.put("聚合间隔", text(data, "aggregationInterval"));
        secondaryFacts.put("最后遥测时间", text(data, "lastTelemetryAt"));

        return result(AssistantReadTool.DEVICE_PRODUCTION, "OK", "已取得设备制水统计（仅统计制水状态开启时的数据）",
                timeRange.description(), selectFacts(invocation.detail(), primaryFacts, secondaryFacts), null);
    }

    private QueryTimeRange productionTimeRange(QueryTimeRange range) {
        if (!range.isAllHistory()) {
            return range;
        }
        LocalDateTime now = LocalDateTime.now();
        return new QueryTimeRange(now.minusDays(1), now, "近24小时至当前（未指定时间范围）");
    }

    private AssistantToolResult alertStats(AssistantToolSchemas.ToolInvocation invocation)
            throws IOException, InterruptedException {
        QueryTimeRange timeRange = toTimeRange(invocation);
        JsonNode data = getInternalData(withTimeRange(
                properties.getMonitorBaseUrl() + "/api/v1/monitor/internal/assistant/alerts/statistics", timeRange));
        Map<String, Object> primaryFacts = new LinkedHashMap<>();
        primaryFacts.put("告警总数", number(data, "totalAlerts"));
        primaryFacts.put("未处理", number(data, "unhandledAlerts"));
        Map<String, Object> secondaryFacts = new LinkedHashMap<>();
        secondaryFacts.put("已处理", number(data, "handledAlerts"));
        secondaryFacts.put("预警级", number(data, "warningAlerts"));
        secondaryFacts.put("报警级", number(data, "alarmAlerts"));
        secondaryFacts.put("已推送", number(data, "pushedAlerts"));
        return result(AssistantReadTool.ALERTS, "OK", "已取得告警统计", timeRange.description(),
                selectFacts(invocation.detail(), primaryFacts, secondaryFacts), null);
    }

    private AssistantToolResult workOrderStats(AssistantToolSchemas.ToolInvocation invocation)
            throws IOException, InterruptedException {
        QueryTimeRange timeRange = toTimeRange(invocation);
        JsonNode data = getInternalData(withTimeRange(
                properties.getWorkOrderBaseUrl() + "/api/v1/work-orders/internal/assistant/statistics", timeRange));
        Map<String, Object> primaryFacts = new LinkedHashMap<>();
        primaryFacts.put("工单总数", number(data, "total"));
        primaryFacts.put("待处理", number(data, "pending"));
        primaryFacts.put("处理中", number(data, "inProgress"));
        primaryFacts.put("已完成", number(data, "completed"));
        Map<String, Object> secondaryFacts = new LinkedHashMap<>();
        secondaryFacts.put("已派单", number(data, "dispatched"));
        secondaryFacts.put("已接单", number(data, "accepted"));
        secondaryFacts.put("已取消", number(data, "cancelled"));
        secondaryFacts.put("平均完成耗时(小时)", decimal(data, "avgCompleteHours", 2));
        return result(AssistantReadTool.WORK_ORDERS, "OK", "已取得工单统计", timeRange.description(),
                selectFacts(invocation.detail(), primaryFacts, secondaryFacts), null);
    }

    private AssistantToolResult scanOrderStats(AssistantToolSchemas.ToolInvocation invocation)
            throws IOException, InterruptedException {
        QueryTimeRange timeRange = toTimeRange(invocation);
        String url = properties.getWaterBaseUrl() + "/api/v1/water/internal/scan-orders/statistics";
        if (!timeRange.isAllHistory()) {
            url += "?paidStartTime=" + URLEncoder.encode(timeRange.startTime().toString(), StandardCharsets.UTF_8)
                    + "&paidEndTime=" + URLEncoder.encode(timeRange.endTime().toString(), StandardCharsets.UTF_8);
        }
        JsonNode data = getInternalData(url);
        Map<String, Object> primaryFacts = new LinkedHashMap<>();
        primaryFacts.put("已支付扫码订单", number(data, "paid"));
        primaryFacts.put("扫码取水收入(元)", centsToYuan(data.path("totalAmount").asLong()));
        primaryFacts.put("扫码取水量(升)", millilitersToLiters(data.path("totalDispenseMl").asLong()));
        Map<String, Object> secondaryFacts = new LinkedHashMap<>();
        secondaryFacts.put("扫码订单总数", number(data, "total"));
        secondaryFacts.put("待支付扫码订单", number(data, "pending"));
        secondaryFacts.put("已下发出水订单", number(data, "dispatched"));
        secondaryFacts.put("命令已发送或确认订单", number(data, "sent"));
        secondaryFacts.put("退款成功订单", number(data, "refunded"));
        secondaryFacts.put("退款中订单", number(data, "refundProcessing"));
        secondaryFacts.put("退款金额(元)", centsToYuan(data.path("refundAmount").asLong()));
        secondaryFacts.put("统计截止时间", LocalDateTime.now().format(QUERY_TIME_FORMAT));
        return result(AssistantReadTool.SCAN_ORDERS, "OK", "已取得扫码取水、退款汇总", timeRange.description(),
                selectFacts(invocation.detail(), primaryFacts, secondaryFacts), null);
    }

    private AssistantToolResult dashboardStats(AssistantToolSchemas.ToolInvocation invocation)
            throws IOException, InterruptedException {
        QueryTimeRange timeRange = toTimeRange(invocation);
        JsonNode data = getInternalData(withTimeRange(
                properties.getReportBaseUrl() + "/api/v1/reports/internal/assistant/dashboard", timeRange));
        Map<String, Object> primaryFacts = new LinkedHashMap<>();
        primaryFacts.put("当前设备总数", number(data, "totalDevices"));
        primaryFacts.put("当前在线设备", number(data, "onlineDevices"));
        primaryFacts.put("统计范围内已支付收入(元)", centsToYuan(data.path("totalRevenue").asLong()));
        primaryFacts.put("统计范围内未处理告警", number(data, "activeAlerts"));
        Map<String, Object> secondaryFacts = new LinkedHashMap<>();
        secondaryFacts.put("当前离线设备", number(data, "offlineDevices"));
        secondaryFacts.put("当前客户总数", number(data, "totalCustomers"));
        secondaryFacts.put("统计范围内订单", number(data, "totalOrders"));
        secondaryFacts.put("统计范围内待处理订单", number(data, "pendingOrders"));
        secondaryFacts.put("滤芯总数", number(data, "totalFilters"));
        secondaryFacts.put("运维人员总数", number(data, "totalWorkers"));
        secondaryFacts.put("经销商总数", number(data, "totalDealers"));
        return result(AssistantReadTool.DASHBOARD, "OK", "已取得平台仪表盘概况", timeRange.description(),
                selectFacts(invocation.detail(), primaryFacts, secondaryFacts), null);
    }

    private AssistantToolResult orderReportStats(AssistantToolSchemas.ToolInvocation invocation)
            throws IOException, InterruptedException {
        QueryTimeRange timeRange = toTimeRange(invocation);
        JsonNode data = getInternalData(withTimeRange(
                properties.getReportBaseUrl() + "/api/v1/reports/internal/assistant/orders", timeRange));
        Map<String, Object> primaryFacts = new LinkedHashMap<>();
        primaryFacts.put("订单总数", number(data, "totalOrders"));
        primaryFacts.put("已支付", number(data, "paidOrders"));
        primaryFacts.put("总收入(元)", centsToYuan(data.path("totalRevenue").asLong()));
        Map<String, Object> secondaryFacts = new LinkedHashMap<>();
        secondaryFacts.put("待支付", number(data, "pendingOrders"));
        secondaryFacts.put("已取消", number(data, "cancelledOrders"));
        secondaryFacts.put("已退款", number(data, "refundedOrders"));
        if (timeRange.isAllHistory()) {
            secondaryFacts.put("今日订单", number(data, "todayOrders"));
            secondaryFacts.put("今日收入(元)", centsToYuan(data.path("todayRevenue").asLong()));
        }
        return result(AssistantReadTool.ORDER_REPORT, "OK", "已取得订单报表统计", timeRange.description(),
                selectFacts(invocation.detail(), primaryFacts, secondaryFacts), null);
    }

    private QueryTimeRange toTimeRange(AssistantToolSchemas.ToolInvocation invocation) {
        if (invocation.startTime() == null || invocation.endTime() == null) {
            return new QueryTimeRange(null, null, "全部历史数据");
        }
        return new QueryTimeRange(invocation.startTime(), invocation.endTime(),
                formatRangeDescription(invocation.startTime(), invocation.endTime()));
    }

    private LocalDateTime parseIsoTime(JsonNode value) {
        if (value == null || !value.isTextual()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.asText().trim(), ISO_LOCAL_DATE_TIME);
        } catch (Exception ex) {
            log.warn("[AssistantTool] Ignored malformed time from model: {}", value.asText());
            return null;
        }
    }

    private String withTimeRange(String url, QueryTimeRange timeRange) {
        if (timeRange.isAllHistory()) {
            return url;
        }
        return url + "?startTime=" + URLEncoder.encode(timeRange.startTime().toString(), StandardCharsets.UTF_8)
                + "&endTime=" + URLEncoder.encode(timeRange.endTime().toString(), StandardCharsets.UTF_8);
    }

    private JsonNode getInternalData(String url) throws IOException, InterruptedException {
        if (!StringUtils.hasText(properties.getInternalServiceToken())) {
            throw new IOException("internal service token is not configured");
        }
        return sendGet(url);
    }

    private JsonNode sendGet(String url) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .header("Accept", "application/json")
                .header("X-Internal-Token", properties.getInternalServiceToken());
        HttpRequest request = requestBuilder.GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("internal HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.path("code").asInt(-1) != 200 || root.path("data").isMissingNode()) {
            throw new IOException("internal response failed");
        }
        return root.path("data");
    }

    private AssistantToolResult result(AssistantReadTool tool, String status, String summary, String dataRange,
                                       Map<String, Object> facts, String unavailableContext) {
        String queriedAt = LocalDateTime.now().format(QUERY_TIME_FORMAT);
        String modelContext = unavailableContext == null
                ? buildModelContext(tool, status, dataRange, queriedAt, facts)
                : "[受控实时数据 - " + tool.title() + "]\n查询状态：" + status + "\n" + unavailableContext;
        return new AssistantToolResult(AdminAssistantDataSourceVO.builder()
                .tool(tool.name())
                .title(tool.title())
                .status(status)
                .summary(summary)
                .dataRange(dataRange)
                .queriedAt(queriedAt)
                .facts(facts)
                .build(), modelContext);
    }

    /**
     * The model decides through the {@code detail} parameter whether secondary
     * metrics are needed; no keyword matching on the raw question.
     */
    private Map<String, Object> selectFacts(boolean detail, Map<String, Object> primaryFacts,
                                            Map<String, Object> secondaryFacts) {
        if (!detail) {
            return primaryFacts;
        }
        Map<String, Object> selected = new LinkedHashMap<>(primaryFacts);
        selected.putAll(secondaryFacts);
        return selected;
    }

    private String buildModelContext(AssistantReadTool tool, String status, String dataRange,
                                     String queriedAt, Map<String, Object> facts) {
        StringBuilder context = new StringBuilder("[受控实时数据 - ").append(tool.title()).append("]\n")
                .append("查询时间：").append(queriedAt).append('\n')
                .append("数据范围：").append(dataRange).append('\n')
                .append("状态：").append(status).append('\n');
        facts.forEach((key, value) -> context.append("- ").append(key).append("：").append(value).append('\n'));
        return context.toString();
    }

    private static String formatRangeDescription(LocalDateTime startTime, LocalDateTime endTime) {
        return startTime.format(QUERY_TIME_FORMAT) + " 至 " + endTime.minusSeconds(1).format(QUERY_TIME_FORMAT);
    }

    private static Object number(JsonNode data, String field) {
        JsonNode value = data.path(field);
        if (value.isNumber()) {
            return value.numberValue();
        }
        if (value.isTextual()) {
            try {
                BigDecimal parsed = new BigDecimal(value.asText().trim()).stripTrailingZeros();
                return parsed.scale() <= 0 ? parsed.longValueExact() : parsed;
            } catch (NumberFormatException | ArithmeticException ignored) {
                // Treat malformed service data as unavailable rather than guessing a value.
            }
        }
        return 0;
    }

    private static Object decimal(JsonNode data, String field, int scale) {
        JsonNode value = data.path(field);
        return value.isNumber() ? value.decimalValue().setScale(scale, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(scale);
    }

    private static Object nullableDecimal(JsonNode data, String field, int scale) {
        JsonNode value = data.path(field);
        return value.isNumber() ? value.decimalValue().setScale(scale, RoundingMode.HALF_UP) : "-";
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).isNull() || node.path(field).isMissingNode() ? "-" : node.path(field).asText("-");
    }

    private static String centsToYuan(long cents) {
        return BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String millilitersToLiters(long milliliters) {
        return BigDecimal.valueOf(milliliters, 3).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private record QueryTimeRange(LocalDateTime startTime, LocalDateTime endTime, String description) {

        boolean isAllHistory() {
            return startTime == null || endTime == null;
        }
    }
}
