package com.platform.report.service.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.platform.report.dto.TelemetryExportRequest;
import com.platform.report.service.ReportService;
import com.platform.report.vo.DashboardVO;
import com.platform.report.vo.DeviceReportVO;
import com.platform.report.vo.FlowReportVO;
import com.platform.report.vo.FinanceReportVO;
import com.platform.report.vo.OrderReportVO;
import com.platform.report.vo.WorkerReportVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 报表统计 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final JdbcTemplate jdbcTemplate;
    private final InfluxDBClient influxDBClient;

    @Value("${influxdb.org:platform}")
    private String influxOrg;

    @Value("${influxdb.bucket:drinking_water}")
    private String influxBucket;

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Duration MAX_EXPORT_RANGE = Duration.ofDays(31);
    private static final int MAX_EXPORT_ROWS = 200_000;
    private static final Pattern SN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern POINT_PATTERN = Pattern.compile("^P\\d{1,3}$");
    private static final Set<String> ALLOWED_INTERVALS = Set.of(
            "1s", "10s", "30s", "1m", "5m", "10m", "30m", "1h", "6h", "12h", "1d"
    );
    private static final Map<String, String> POINT_LABELS = createPointLabels();

    @Override
    public DashboardVO getDashboard() {
        return getDashboard(null, null);
    }

    @Override
    public DashboardVO getDashboard(LocalDateTime startTime, LocalDateTime endTime) {
        DashboardVO vo = new DashboardVO();
        vo.setTotalDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE deleted = 0"));
        vo.setOnlineDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 1 AND deleted = 0"));
        vo.setOfflineDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 0 AND deleted = 0"));
        vo.setTotalCustomers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM customer WHERE deleted = 0"));
        vo.setTotalOrders(queryForLongInRange(
                "SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE deleted = 0", "ordered_at", startTime, endTime));
        vo.setTotalRevenue(queryForLongInRange(
                "SELECT COALESCE(SUM(pay_amount), 0) FROM order_info WHERE order_status = 'PAID' AND deleted = 0", "paid_at", startTime, endTime));
        vo.setPendingOrders(queryForLongInRange(
                "SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'PENDING' AND deleted = 0", "ordered_at", startTime, endTime));
        vo.setTotalFilters(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM filter_instance WHERE deleted = 0"));
        vo.setTotalWorkers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM worker WHERE deleted = 0"));
        vo.setTotalDealers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM dealer WHERE deleted = 0"));
        vo.setActiveAlerts(queryForLongInRange(
                "SELECT COALESCE(COUNT(*), 0) FROM device_alert WHERE handled_status = 'UNHANDLED' AND deleted = 0", "triggered_at", startTime, endTime));
        log.info("[报表] 仪表盘统计完成");
        return vo;
    }

    @Override
    public DeviceReportVO getDeviceReport() {
        return getDeviceReport(null, null);
    }

    @Override
    public DeviceReportVO getDeviceReport(LocalDateTime startTime, LocalDateTime endTime) {
        DeviceReportVO vo = new DeviceReportVO();
        vo.setTotalDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE deleted = 0"));
        vo.setOnlineCount(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 1 AND deleted = 0"));
        vo.setOfflineCount(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 0 AND deleted = 0"));
        vo.setFaultCount(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE lifecycle_status = 'FAULT' AND deleted = 0"));
        if (startTime == null) {
            vo.setRegisteredToday(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE DATE(created_at) = CURDATE() AND deleted = 0"));
            vo.setActivatedToday(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE DATE(activated_at) = CURDATE() AND deleted = 0"));
        } else {
            vo.setRegisteredToday(queryForLongInRange(
                    "SELECT COALESCE(COUNT(*), 0) FROM device WHERE deleted = 0", "created_at", startTime, endTime));
            vo.setActivatedToday(queryForLongInRange(
                    "SELECT COALESCE(COUNT(*), 0) FROM device WHERE deleted = 0", "activated_at", startTime, endTime));
        }
        log.info("[报表] 设备统计完成");
        return vo;
    }

    @Override
    public OrderReportVO getOrderReport() {
        return getOrderReport(null, null);
    }

    @Override
    public OrderReportVO getOrderReport(LocalDateTime startTime, LocalDateTime endTime) {
        OrderReportVO vo = new OrderReportVO();
        vo.setTotalOrders(queryForLongInRange(
                "SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE deleted = 0", "ordered_at", startTime, endTime));
        vo.setPendingOrders(queryForLongInRange(
                "SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'PENDING' AND deleted = 0", "ordered_at", startTime, endTime));
        vo.setPaidOrders(queryForLongInRange(
                "SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'PAID' AND deleted = 0", "ordered_at", startTime, endTime));
        vo.setCancelledOrders(queryForLongInRange(
                "SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'CANCELLED' AND deleted = 0", "ordered_at", startTime, endTime));
        vo.setRefundedOrders(queryForLongInRange(
                "SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'REFUNDED' AND deleted = 0", "ordered_at", startTime, endTime));
        vo.setTotalRevenue(queryForLongInRange(
                "SELECT COALESCE(SUM(pay_amount), 0) FROM order_info WHERE order_status = 'PAID' AND deleted = 0", "paid_at", startTime, endTime));
        vo.setTodayOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE DATE(created_at) = CURDATE() AND deleted = 0"));
        vo.setTodayRevenue(queryForLong("SELECT COALESCE(SUM(pay_amount), 0) FROM order_info WHERE order_status = 'PAID' AND DATE(created_at) = CURDATE() AND deleted = 0"));
        log.info("[报表] 订单统计完成");
        return vo;
    }

    @Override
    public FinanceReportVO getFinanceReport() {
        FinanceReportVO vo = new FinanceReportVO();
        vo.setTotalCommission(queryForLong("SELECT COALESCE(SUM(commission_amount), 0) FROM commission_record WHERE deleted = 0"));
        vo.setSettledCommission(queryForLong("SELECT COALESCE(SUM(commission_amount), 0) FROM commission_record WHERE status = 'SETTLED' AND deleted = 0"));
        vo.setPendingCommission(queryForLong("SELECT COALESCE(SUM(commission_amount), 0) FROM commission_record WHERE status = 'PENDING' AND deleted = 0"));
        vo.setTotalConsumption(queryForLong("SELECT COALESCE(SUM(revenue_amount), 0) FROM consumption_record WHERE deleted = 0"));
        vo.setTotalRevenue(queryForLong("SELECT COALESCE(SUM(pay_amount), 0) FROM order_info WHERE order_status = 'PAID' AND deleted = 0"));
        vo.setPendingSettlements(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM settlement_bill WHERE status = 'PENDING' AND deleted = 0"));
        log.info("[报表] 财务统计完成");
        return vo;
    }

    @Override
    public WorkerReportVO getWorkerReport() {
        WorkerReportVO vo = new WorkerReportVO();
        vo.setTotalWorkers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM worker WHERE deleted = 0"));
        vo.setActiveWorkers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM worker WHERE status = 'ACTIVE' AND deleted = 0"));
        vo.setTotalWorkOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM work_order WHERE deleted = 0"));
        vo.setPendingWorkOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM work_order WHERE order_status IN ('PENDING', 'DISPATCHED', 'ACCEPTED') AND deleted = 0"));
        vo.setCompletedWorkOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM work_order WHERE order_status = 'COMPLETED' AND deleted = 0"));
        vo.setAvgRating(queryForLong("SELECT COALESCE(ROUND(AVG(rating)), 0) FROM worker WHERE deleted = 0"));
        log.info("[报表] 维修工统计完成");
        return vo;
    }

    // ==================== 私有方法 ====================

    /**
     * 查询单个 Long 值，null 返回 0
     */
    private Long queryForLong(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    /** Timestamp column names are compile-time constants supplied by this service only. */
    private Long queryForLongInRange(String sql, String timestampColumn,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return queryForLong(sql);
        }
        Long result = jdbcTemplate.queryForObject(
                sql + " AND " + timestampColumn + " >= ? AND " + timestampColumn + " < ?",
                Long.class, startTime, endTime);
        return result != null ? result : 0L;
    }

    @Override
    public FlowReportVO getFlowReport() {
        FlowReportVO vo = new FlowReportVO();
        vo.setTotalDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE deleted = 0"));
        vo.setOnlineDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 1 AND deleted = 0"));
        vo.setOfflineDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 0 AND deleted = 0"));
        vo.setTodayReportDevices(queryForLong(
                "SELECT COALESCE(COUNT(DISTINCT device_id), 0) FROM consumption_record WHERE DATE(consumed_at) = CURDATE() AND deleted = 0"));
        vo.setTotalWaterFlow(queryForLong(
                "SELECT COALESCE(SUM(consume_flow), 0) FROM consumption_record WHERE deleted = 0"));
        vo.setTodayWaterFlow(queryForLong(
                "SELECT COALESCE(SUM(consume_flow), 0) FROM consumption_record WHERE DATE(consumed_at) = CURDATE() AND deleted = 0"));
        vo.setTotalPureFlow(vo.getTotalWaterFlow());
        vo.setTodayPureFlow(vo.getTodayWaterFlow());

        // 在线率
        long total = vo.getTotalDevices() != null ? vo.getTotalDevices() : 0L;
        long online = vo.getOnlineDevices() != null ? vo.getOnlineDevices() : 0L;
        vo.setOnlineRate(total > 0 ? Math.round(online * 1000.0 / total) / 10.0 : 0.0);

        // 近7天用水量趋势
        List<Map<String, Object>> dailyFlow = new ArrayList<>();
        List<Map<String, Object>> flowRows = jdbcTemplate.queryForList(
                "SELECT DATE(consumed_at) as flow_date, COALESCE(SUM(consume_flow), 0) as total_flow " +
                "FROM consumption_record WHERE consumed_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND deleted = 0 " +
                "GROUP BY DATE(consumed_at) ORDER BY flow_date");
        java.time.LocalDate today = java.time.LocalDate.now();
        Map<String, Long> flowMap = new HashMap<>();
        for (Map<String, Object> row : flowRows) {
            flowMap.put(String.valueOf(row.get("flow_date")),
                    row.get("total_flow") != null ? ((Number) row.get("total_flow")).longValue() : 0L);
        }
        for (int i = 6; i >= 0; i--) {
            String dateStr = today.minusDays(i).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);
            item.put("value", flowMap.getOrDefault(dateStr, 0L));
            dailyFlow.add(item);
        }
        vo.setDailyFlowTrend(dailyFlow);

        // 设备型号分布
        List<Map<String, Object>> modelDist = new ArrayList<>();
        List<Map<String, Object>> modelRows = jdbcTemplate.queryForList(
                "SELECT dm.model_name, COUNT(d.id) as cnt FROM device d " +
                "JOIN device_model dm ON d.model_id = dm.id AND dm.deleted = 0 " +
                "WHERE d.deleted = 0 GROUP BY dm.model_name ORDER BY cnt DESC LIMIT 10");
        for (Map<String, Object> row : modelRows) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", row.get("model_name"));
            item.put("value", row.get("cnt"));
            modelDist.add(item);
        }
        vo.setModelDistribution(modelDist);

        log.info("[报表] 流量统计完成");
        return vo;
    }

    @Override
    public void exportTelemetry(TelemetryExportRequest request, HttpServletResponse response) throws IOException {
        DeviceIdentity device = resolveDevice(request.getDeviceId(), request.getSn());
        List<String> fields = normalizeFields(request.getFields());
        String interval = normalizeInterval(request.getInterval());
        Instant start = parseTime(request.getStartTime());
        Instant end = parseTime(request.getEndTime());

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        if (Duration.between(start, end).compareTo(MAX_EXPORT_RANGE) > 0) {
            throw new IllegalArgumentException("单次导出时间范围不能超过 31 天");
        }

        validateEstimatedExportSize(fields, start, end, interval);

        TreeMap<Instant, Map<String, Object>> rows = queryTelemetryRows(
                "device_id", device.deviceId(), fields, start, end, interval);
        writeTelemetryWorkbook(device, fields, start, end, interval, rows, response);
        log.info("[报表] 设备历史数据导出完成, deviceId={}, sn={}, fields={}, rows={}",
                device.deviceId(), device.sn(), fields, rows.size());
    }

    private TreeMap<Instant, Map<String, Object>> queryTelemetryRows(
            String tagName, String tagValue, List<String> fields, Instant start, Instant end, String interval) {
        String fieldList = fields.stream()
                .map(field -> "\"" + field + "\"")
                .collect(Collectors.joining(", "));
        String flux = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: time(v: \"%s\"), stop: time(v: \"%s\"))\n" +
                "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.%s == \"%s\")\n" +
                "  |> filter(fn: (r) => contains(value: r._field, set: [%s]))\n" +
                "  |> filter(fn: (r) => exists r._value)\n" +
                "  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)\n" +
                "  |> keep(columns: [\"_time\", \"_field\", \"_value\"])",
                escapeFluxString(influxBucket), start, end, tagName, escapeFluxString(tagValue), fieldList, interval
        );

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxOrg);
        TreeMap<Instant, Map<String, Object>> rows = new TreeMap<>();
        int recordCount = 0;
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                if (record.getTime() == null || record.getField() == null) {
                    continue;
                }
                rows.computeIfAbsent(record.getTime(), key -> new LinkedHashMap<>())
                        .put(record.getField(), record.getValue());
                recordCount++;
                if (recordCount > MAX_EXPORT_ROWS) {
                    throw new IllegalArgumentException("导出数据量过大，请缩短时间范围或调大统计间隔");
                }
            }
        }
        return rows;
    }

    private void writeTelemetryWorkbook(
            DeviceIdentity device,
            List<String> fields,
            Instant start,
            Instant end,
            String interval,
            TreeMap<Instant, Map<String, Object>> dataRows,
            HttpServletResponse response) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("历史数据");
            sheet.setColumnWidth(0, 22 * 256);
            for (int i = 0; i < fields.size(); i++) {
                sheet.setColumnWidth(i + 1, 20 * 256);
            }

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("设备历史数据导出");
            titleRow.getCell(0).setCellStyle(titleStyle);

            Row metaRow = sheet.createRow(1);
            metaRow.createCell(0).setCellValue("设备ID");
            metaRow.createCell(1).setCellValue(device.deviceId());
            metaRow.createCell(2).setCellValue("当前控制板SN");
            metaRow.createCell(3).setCellValue(device.sn() == null ? "-" : device.sn());
            metaRow.createCell(4).setCellValue("时间范围");
            metaRow.createCell(5).setCellValue(formatInstant(start) + " 至 " + formatInstant(end));
            metaRow.createCell(6).setCellValue("间隔");
            metaRow.createCell(7).setCellValue(interval);

            Row headerRow = sheet.createRow(3);
            headerRow.createCell(0).setCellValue("时间");
            headerRow.getCell(0).setCellStyle(headerStyle);
            for (int i = 0; i < fields.size(); i++) {
                headerRow.createCell(i + 1).setCellValue(getPointLabel(fields.get(i)));
                headerRow.getCell(i + 1).setCellStyle(headerStyle);
            }

            int rowIndex = 4;
            for (Map.Entry<Instant, Map<String, Object>> entry : dataRows.entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(formatInstant(entry.getKey()));
                Map<String, Object> values = entry.getValue();
                for (int i = 0; i < fields.size(); i++) {
                    Object value = values.get(fields.get(i));
                    if (value instanceof Number number) {
                        row.createCell(i + 1).setCellValue(number.doubleValue());
                    } else if (value != null) {
                        row.createCell(i + 1).setCellValue(String.valueOf(value));
                    } else {
                        row.createCell(i + 1).setCellValue("");
                    }
                }
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String filename = "设备历史数据_" + device.deviceId() + "_" +
                    LocalDateTime.now(CHINA_ZONE).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            workbook.write(response.getOutputStream());
        }
    }

    private String normalizeSn(String sn) {
        String normalized = sn == null ? "" : sn.trim();
        if (!SN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("设备 SN 格式不正确");
        }
        return normalized;
    }

    private DeviceIdentity resolveDevice(String deviceId, String requestedSn) {
        String normalizedDeviceId = deviceId == null ? "" : deviceId.trim();
        if (normalizedDeviceId.isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT device_id, sn FROM device WHERE device_id = ? AND deleted = 0 LIMIT 1",
                normalizedDeviceId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("设备不存在");
        }
        String currentSn = rows.get(0).get("sn") == null ? null : String.valueOf(rows.get(0).get("sn"));
        if (currentSn == null && requestedSn != null && !requestedSn.isBlank()) {
            currentSn = normalizeSn(requestedSn);
        }
        return new DeviceIdentity(normalizedDeviceId, currentSn);
    }

    private record DeviceIdentity(String deviceId, String sn) {
    }

    private List<String> normalizeFields(String fields) {
        if (fields == null || fields.isBlank()) {
            throw new IllegalArgumentException("请选择导出点位");
        }
        LinkedHashSet<String> normalized = Arrays.stream(fields.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(field -> !field.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("请选择导出点位");
        }
        if (normalized.size() > 30) {
            throw new IllegalArgumentException("单次最多导出 30 个点位");
        }
        for (String field : normalized) {
            if (!POINT_PATTERN.matcher(field).matches()) {
                throw new IllegalArgumentException("点位格式不正确: " + field);
            }
        }
        return new ArrayList<>(normalized);
    }

    private String normalizeInterval(String interval) {
        String normalized = interval == null || interval.isBlank() ? "10m" : interval.trim().toLowerCase();
        if (!ALLOWED_INTERVALS.contains(normalized)) {
            throw new IllegalArgumentException("不支持的统计间隔: " + interval);
        }
        return normalized;
    }

    private void validateEstimatedExportSize(List<String> fields, Instant start, Instant end, String interval) {
        Duration intervalDuration = parseIntervalDuration(interval);
        long seconds = Math.max(1L, Duration.between(start, end).getSeconds());
        long intervalSeconds = Math.max(1L, intervalDuration.getSeconds());
        long estimatedRecords = ((seconds + intervalSeconds - 1) / intervalSeconds) * fields.size();
        if (estimatedRecords > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("导出数据量过大，请缩短时间范围或调大统计间隔");
        }
    }

    private Duration parseIntervalDuration(String interval) {
        String unit = interval.substring(interval.length() - 1);
        long amount = Long.parseLong(interval.substring(0, interval.length() - 1));
        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> Duration.ofMinutes(10);
        };
    }

    private Instant parseTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("时间不能为空");
        }
        String text = value.trim();
        try {
            if (text.endsWith("Z") || text.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return Instant.parse(text);
            }
            DateTimeFormatter formatter = text.length() == 16
                    ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(text.replace('T', ' '), formatter).atZone(CHINA_ZONE).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("时间格式不正确，请使用 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String formatInstant(Instant instant) {
        return DISPLAY_TIME_FORMATTER.format(instant.atZone(CHINA_ZONE));
    }

    private String getPointLabel(String pointId) {
        return pointId + " - " + POINT_LABELS.getOrDefault(pointId, pointId);
    }

    private String escapeFluxString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Map<String, String> createPointLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("P1", "原水TDS (PPM)");
        labels.put("P2", "纯水TDS (PPM)");
        labels.put("P3", "矿水TDS (PPM)");
        labels.put("P4", "原水温度 (℃)");
        labels.put("P5", "纯水温度 (℃)");
        labels.put("P6", "矿水温度 (℃)");
        labels.put("P7", "纯水瞬时流量 (L/min)");
        labels.put("P8", "净水瞬时流量 (L/min)");
        labels.put("P9", "矿水瞬时流量 (L/min)");
        labels.put("P10", "废水瞬时流量 (L/min)");
        labels.put("P11", "原水瞬时流量 (L/min)");
        labels.put("P12", "纯水累计流量 (L)");
        labels.put("P13", "净水累计流量 (L)");
        labels.put("P14", "矿水累计流量 (L)");
        labels.put("P15", "废水累计流量 (L)");
        labels.put("P16", "原水累计流量 (L)");
        labels.put("P17", "原水压力 (bar)");
        labels.put("P18", "膜前压力 (bar)");
        labels.put("P19", "膜后压力 (bar)");
        labels.put("P20", "矿水压力 (bar)");
        labels.put("P21", "比例阀开度1 (%)");
        labels.put("P22", "比例阀开度2 (%)");
        labels.put("P23", "制水状态");
        labels.put("P24", "TDS制水状态");
        labels.put("P25", "RO强冲状态");
        labels.put("P26", "纯水洗膜状态");
        labels.put("P27", "超滤冲洗状态");
        labels.put("P28", "故障报警");
        labels.put("P29", "比例阀状态");
        labels.put("P30", "预留3");
        labels.put("P31", "高压开关");
        labels.put("P32", "低压开关");
        labels.put("P33", "漏水状态");
        labels.put("P34", "电压低检测");
        labels.put("P35", "DI1");
        labels.put("P36", "DI2");
        labels.put("P37", "DI3");
        labels.put("P38", "DI4");
        labels.put("P39", "DI5");
        return labels;
    }
}
