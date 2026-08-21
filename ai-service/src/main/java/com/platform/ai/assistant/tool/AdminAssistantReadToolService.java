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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controlled data access for the admin assistant.
 *
 * <p>Every request is a fixed, internal GET endpoint selected by code. The
 * model never receives database credentials, a request URL, raw page rows, or
 * any ability to create a write request.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAssistantReadToolService {

    private static final DateTimeFormatter QUERY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern LABELED_DEVICE_IDENTIFIER = Pattern.compile("(?i)(?:\\bsn\\b|设备(?:id|编号)?)[：: ]*([a-z0-9_-]{2,})");
    private static final Pattern CONTROL_BOARD_SN = Pattern.compile("(?i)\\b(j[0-9]{5,})\\b");
    private static final Pattern ISO_DATE_TIME = Pattern.compile(
            "(?<!\\d)(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})(?:[T\\s]+(\\d{1,2})(?::(\\d{1,2}))?(?::(\\d{1,2}))?)?");
    private static final Pattern CHINESE_DATE_TIME = Pattern.compile(
            "(?:(20\\d{2})年)?(\\d{1,2})月(\\d{1,2})(?:日|号)?(?:\\s*(\\d{1,2})[点时](?:(\\d{1,2})分?)?)?");
    private static final Pattern ROLLING_RANGE = Pattern.compile(
            "(?:近|最近|过去)([0-9一二三四五六七八九十两]+)\\s*(?:个)?(分钟|小时|天|日|周|星期|个月|月)");
    private static final Pattern IMPLIED_DURATION = Pattern.compile(
            "(?<![0-9一二三四五六七八九十两])([0-9一二三四五六七八九十两]+)\\s*(分钟|小时|天|日|周|星期|个月|月)(?:内|的|以来|情况|数据|统计|详情|订单|告警|工单|收入|取水)");
    private static final int MAX_TOOLS_PER_QUESTION = 3;

    private final AdminAssistantToolProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public List<AssistantToolResult> collect(String question, CurrentUser user) {
        if (!properties.isEnabled()) {
            return List.of();
        }
        List<AssistantToolResult> results = new ArrayList<>();
        for (AssistantReadTool tool : selectTools(question)) {
            results.add(execute(tool, question, user));
        }
        return results;
    }

    private Set<AssistantReadTool> selectTools(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        EnumSet<AssistantReadTool> selected = EnumSet.noneOf(AssistantReadTool.class);
        if (containsAny(normalized, "告警", "报警", "预警", "未处理")) {
            selected.add(AssistantReadTool.ALERTS);
        }
        if (containsAny(normalized, "工单", "报修", "派单", "接单", "维修", "退机", "移机")) {
            selected.add(AssistantReadTool.WORK_ORDERS);
        }
        if (containsAny(normalized, "扫码", "取水", "出水", "退款", "支付", "q74", "ack")) {
            selected.add(AssistantReadTool.SCAN_ORDERS);
        }
        if (!containsAny(normalized, "扫码", "取水", "出水")
                && containsAny(normalized, "营收", "收入", "订单报表", "今日订单", "今日收入")) {
            selected.add(AssistantReadTool.ORDER_REPORT);
        }
        if (containsAny(normalized, "仪表盘", "概览", "总览", "经营", "全局")) {
            selected.add(AssistantReadTool.DASHBOARD);
        }
        if (containsAny(normalized, "设备", "在线", "离线", "控制板", "心跳", "型号", "sn")) {
            selected.add(AssistantReadTool.DEVICE);
        }
        if (containsAny(normalized, "制水", "产水", "原水tds", "纯水tds", "累计流量", "膜前", "膜后", "废水比")) {
            selected.add(AssistantReadTool.DEVICE_PRODUCTION);
        }
        if (selected.isEmpty()) {
            return Set.of();
        }
        EnumSet<AssistantReadTool> limited = EnumSet.noneOf(AssistantReadTool.class);
        for (AssistantReadTool tool : selected) {
            if (limited.size() >= MAX_TOOLS_PER_QUESTION) {
                break;
            }
            limited.add(tool);
        }
        return limited;
    }

    private AssistantToolResult execute(AssistantReadTool tool, String question, CurrentUser user) {
        if (!user.hasPermission(tool.permission())) {
            return result(tool, "DENIED", "当前账号没有此数据的查看权限", "无", Map.of(),
                    "当前用户无权访问该数据。不得推断或补充任何相关业务数据。");
        }
        try {
            return switch (tool) {
                case DEVICE -> deviceSnapshot(question);
                case DEVICE_PRODUCTION -> deviceProductionStats(question);
                case ALERTS -> alertStats(question);
                case WORK_ORDERS -> workOrderStats(question);
                case SCAN_ORDERS -> scanOrderStats(question);
                case DASHBOARD -> dashboardStats(question);
                case ORDER_REPORT -> orderReportStats(question);
            };
        } catch (Exception ex) {
            log.warn("[AssistantTool] Read failed, userId={}, tool={}, error={}",
                    user.getUserId(), tool.name(), ex.getClass().getSimpleName());
            return result(tool, "UNAVAILABLE", "暂时无法取得该数据，请稍后在对应业务页面核对", "当前查询失败", Map.of(),
                    "该受控数据暂时不可用，不得将其当作正常或异常数据进行推断。");
        }
    }

    private AssistantToolResult deviceSnapshot(String question) throws IOException, InterruptedException {
        String identifier = extractDeviceIdentifier(question);
        QueryTimeRange timeRange = resolveTimeRange(question);
        if (StringUtils.hasText(identifier)) {
            JsonNode record = getInternalData(properties.getDeviceBaseUrl() + "/api/v1/devices/internal/assistant/device?identifier="
                    + URLEncoder.encode(identifier, StandardCharsets.UTF_8));
            if (!record.path("found").asBoolean(false)) {
                return result(AssistantReadTool.DEVICE, "NOT_FOUND", "未找到匹配设备", "当前设备查询", Map.of("查询标识", identifier),
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
                    selectFacts(question, primaryFacts, secondaryFacts, "激活", "生命周期", "登记"), null);
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
                selectFacts(question, primaryFacts, secondaryFacts, "故障", "激活", "登记"), null);
    }

    private AssistantToolResult deviceProductionStats(String question) throws IOException, InterruptedException {
        String identifier = extractDeviceIdentifier(question);
        if (!StringUtils.hasText(identifier)) {
            return result(AssistantReadTool.DEVICE_PRODUCTION, "NOT_FOUND", "请提供设备ID或控制板SN", "当前设备查询", Map.of(),
                    "未提供可识别的设备ID或控制板SN，不能查询设备制水历史数据。");
        }

        JsonNode device = getInternalData(properties.getDeviceBaseUrl() + "/api/v1/devices/internal/assistant/device?identifier="
                + URLEncoder.encode(identifier, StandardCharsets.UTF_8));
        if (!device.path("found").asBoolean(false)) {
            return result(AssistantReadTool.DEVICE_PRODUCTION, "NOT_FOUND", "未找到匹配设备", "当前设备查询",
                    Map.of("查询标识", identifier), "未找到匹配设备，不能据此判断制水数据。");
        }

        QueryTimeRange timeRange = productionTimeRange(resolveTimeRange(question));
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
                timeRange.description(), selectProductionFacts(question, primaryFacts, secondaryFacts), null);
    }

    private QueryTimeRange productionTimeRange(QueryTimeRange range) {
        if (!range.isAllHistory()) {
            return range;
        }
        LocalDateTime now = LocalDateTime.now();
        return new QueryTimeRange(now.minusDays(1), now, "近24小时至当前（未指定时间范围）");
    }

    private Map<String, Object> selectProductionFacts(String question, Map<String, Object> primaryFacts,
                                                       Map<String, Object> secondaryFacts) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "详情", "明细", "详细", "全部", "所有", "指标", "tds", "水质", "压力", "废水", "流量")) {
            Map<String, Object> allFacts = new LinkedHashMap<>(primaryFacts);
            allFacts.putAll(secondaryFacts);
            return allFacts;
        }
        return primaryFacts;
    }

    private AssistantToolResult alertStats(String question) throws IOException, InterruptedException {
        QueryTimeRange timeRange = resolveTimeRange(question);
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
                selectFacts(question, primaryFacts, secondaryFacts, "级别", "预警", "报警", "推送", "已处理"), null);
    }

    private AssistantToolResult workOrderStats(String question) throws IOException, InterruptedException {
        QueryTimeRange timeRange = resolveTimeRange(question);
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
                selectFacts(question, primaryFacts, secondaryFacts, "派单", "接单", "取消", "耗时", "平均"), null);
    }

    private AssistantToolResult scanOrderStats(String question) throws IOException, InterruptedException {
        QueryTimeRange timeRange = resolveTimeRange(question);
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
                selectFacts(question, primaryFacts, secondaryFacts,
                        "退款", "待支付", "下发", "命令", "q74", "ack", "支付状态", "出水状态"), null);
    }

    private QueryTimeRange resolveTimeRange(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        List<ParsedDateTime> explicitDates = extractExplicitDates(question, now.getYear());
        if (explicitDates.size() >= 2) {
            ParsedDateTime start = explicitDates.get(0);
            ParsedDateTime end = explicitDates.get(1);
            LocalDateTime endTime = end.exclusiveEnd();
            if (!endTime.isAfter(start.value())) {
                throw new IllegalArgumentException("结束时间必须晚于开始时间");
            }
            return new QueryTimeRange(start.value(), endTime,
                    formatRangeDescription(start.value(), endTime));
        }
        if (explicitDates.size() == 1) {
            LocalDateTime start = explicitDates.get(0).value().toLocalDate().atStartOfDay();
            return new QueryTimeRange(start, start.plusDays(1),
                    start.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + " 全天");
        }
        if (containsAny(normalized, "今天", "今日")) {
            return new QueryTimeRange(now.toLocalDate().atStartOfDay(), now, "今天 00:00 至当前");
        }
        if (containsAny(normalized, "昨天", "昨日")) {
            LocalDate yesterday = now.toLocalDate().minusDays(1);
            return new QueryTimeRange(yesterday.atStartOfDay(), now.toLocalDate().atStartOfDay(), "昨天全天");
        }
        if (containsAny(normalized, "上周", "上一周")) {
            LocalDate today = now.toLocalDate();
            LocalDate start = today.with(DayOfWeek.MONDAY).minusWeeks(1);
            return new QueryTimeRange(start.atStartOfDay(), start.plusWeeks(1).atStartOfDay(), "上周全天");
        }
        if (containsAny(normalized, "上个月", "上月")) {
            LocalDate firstDay = now.toLocalDate().withDayOfMonth(1).minusMonths(1);
            return new QueryTimeRange(firstDay.atStartOfDay(), firstDay.plusMonths(1).atStartOfDay(), "上月全天");
        }
        if (normalized.contains("本周")) {
            LocalDate today = now.toLocalDate();
            return new QueryTimeRange(today.with(DayOfWeek.MONDAY).atStartOfDay(), now, "本周至当前");
        }
        if (normalized.contains("本月")) {
            return new QueryTimeRange(now.toLocalDate().withDayOfMonth(1).atStartOfDay(), now, "本月至当前");
        }
        Matcher rollingMatcher = ROLLING_RANGE.matcher(normalized);
        boolean hasRollingRange = rollingMatcher.find();
        if (!hasRollingRange) {
            rollingMatcher = IMPLIED_DURATION.matcher(normalized);
            hasRollingRange = rollingMatcher.find();
        }
        if (hasRollingRange) {
            int amount = parseNaturalNumber(rollingMatcher.group(1));
            if (amount <= 0) {
                throw new IllegalArgumentException("时间范围无效");
            }
            String unit = rollingMatcher.group(2);
            LocalDateTime start = switch (unit) {
                case "分钟" -> now.minusMinutes(amount);
                case "小时" -> now.minusHours(amount);
                case "周", "星期" -> now.minusWeeks(amount);
                case "个月", "月" -> now.minusMonths(amount);
                default -> now.minusDays(amount);
            };
            return new QueryTimeRange(start, now, "近 " + amount + unit + "至当前");
        }
        if (containsAny(normalized, "近一周", "最近一周")) {
            return new QueryTimeRange(now.minusDays(7), now, "近 7 天至当前");
        }
        if (containsAny(normalized, "近一个月", "最近一个月")) {
            return new QueryTimeRange(now.minusMonths(1), now, "近 1 个月至当前");
        }
        return new QueryTimeRange(null, null, "全部历史数据");
    }

    private AssistantToolResult dashboardStats(String question) throws IOException, InterruptedException {
        QueryTimeRange timeRange = resolveTimeRange(question);
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
                selectFacts(question, primaryFacts, secondaryFacts,
                        "客户", "订单", "滤芯", "运维", "经销商", "离线", "待处理"), null);
    }

    private AssistantToolResult orderReportStats(String question) throws IOException, InterruptedException {
        QueryTimeRange timeRange = resolveTimeRange(question);
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
                selectFacts(question, primaryFacts, secondaryFacts, "待支付", "取消", "退款", "今日"), null);
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
     * Keeps routine answers focused while allowing an explicit detail question
     * to reveal the complete aggregate set already approved for this tool.
     */
    private Map<String, Object> selectFacts(String question, Map<String, Object> primaryFacts,
                                            Map<String, Object> secondaryFacts, String... secondaryKeywords) {
        Map<String, Object> selected = new LinkedHashMap<>(primaryFacts);
        if (asksForSecondaryMetrics(question, secondaryKeywords)) {
            selected.putAll(secondaryFacts);
        }
        return selected;
    }

    private boolean asksForSecondaryMetrics(String question, String... secondaryKeywords) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        return containsAny(normalized, "详情", "明细", "详细", "全部", "所有", "各项", "指标", "分布", "状态", "异常", "原因")
                || containsAny(normalized, secondaryKeywords);
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

    private String extractDeviceIdentifier(String question) {
        if (!StringUtils.hasText(question)) {
            return null;
        }
        Matcher labeled = LABELED_DEVICE_IDENTIFIER.matcher(question);
        if (labeled.find()) {
            return labeled.group(1);
        }
        Matcher sn = CONTROL_BOARD_SN.matcher(question);
        return sn.find() ? sn.group(1) : null;
    }

    private List<ParsedDateTime> extractExplicitDates(String question, int defaultYear) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        List<ParsedDateTime> values = new ArrayList<>();
        Matcher isoMatcher = ISO_DATE_TIME.matcher(question);
        while (isoMatcher.find()) {
            values.add(buildParsedDateTime(isoMatcher.start(),
                    isoMatcher.group(1), isoMatcher.group(2), isoMatcher.group(3),
                    isoMatcher.group(4), isoMatcher.group(5), isoMatcher.group(6), defaultYear));
        }
        Matcher chineseMatcher = CHINESE_DATE_TIME.matcher(question);
        while (chineseMatcher.find()) {
            values.add(buildParsedDateTime(chineseMatcher.start(),
                    chineseMatcher.group(1), chineseMatcher.group(2), chineseMatcher.group(3),
                    chineseMatcher.group(4), chineseMatcher.group(5), null, defaultYear));
        }
        values.sort(java.util.Comparator.comparingInt(ParsedDateTime::position));
        return values;
    }

    private ParsedDateTime buildParsedDateTime(int position, String yearText, String monthText, String dayText,
                                               String hourText, String minuteText, String secondText, int defaultYear) {
        int year = StringUtils.hasText(yearText) ? Integer.parseInt(yearText) : defaultYear;
        int month = Integer.parseInt(monthText);
        int day = Integer.parseInt(dayText);
        int hour = StringUtils.hasText(hourText) ? Integer.parseInt(hourText) : 0;
        int minute = StringUtils.hasText(minuteText) ? Integer.parseInt(minuteText) : 0;
        int second = StringUtils.hasText(secondText) ? Integer.parseInt(secondText) : 0;
        int precision = !StringUtils.hasText(hourText) ? 0
                : !StringUtils.hasText(minuteText) ? 1
                : !StringUtils.hasText(secondText) ? 2 : 3;
        return new ParsedDateTime(LocalDateTime.of(year, month, day, hour, minute, second), precision, position);
    }

    private static int parseNaturalNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        if (value.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(value);
        }
        int total = 0;
        int current = 0;
        for (char character : value.toCharArray()) {
            int digit = switch (character) {
                case '一' -> 1;
                case '二', '两' -> 2;
                case '三' -> 3;
                case '四' -> 4;
                case '五' -> 5;
                case '六' -> 6;
                case '七' -> 7;
                case '八' -> 8;
                case '九' -> 9;
                default -> 0;
            };
            if (character == '十') {
                total += (current == 0 ? 1 : current) * 10;
                current = 0;
            } else {
                current = digit;
            }
        }
        return total + current;
    }

    private static String formatRangeDescription(LocalDateTime startTime, LocalDateTime endTime) {
        return startTime.format(QUERY_TIME_FORMAT) + " 至 " + endTime.minusSeconds(1).format(QUERY_TIME_FORMAT);
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
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

    private record ParsedDateTime(LocalDateTime value, int precision, int position) {

        LocalDateTime exclusiveEnd() {
            return switch (precision) {
                case 0 -> value.toLocalDate().plusDays(1).atStartOfDay();
                case 1 -> value.plusHours(1);
                case 2 -> value.plusMinutes(1);
                default -> value.plusSeconds(1);
            };
        }
    }
}
