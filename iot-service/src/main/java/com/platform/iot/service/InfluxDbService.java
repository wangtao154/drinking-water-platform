package com.platform.iot.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxDbService {

    private final InfluxDBClient influxDBClient;

    @Value("${influxdb.org:platform}")
    private String influxOrg;

    @Value("${influxdb.bucket:drinking_water}")
    private String influxBucket;

    private static final Set<String> ALLOWED_HISTORY_INTERVALS = Set.of(
            "1s", "10s", "30s", "1m", "5m", "10m", "30m", "1h", "6h", "12h", "1d"
    );

    private static final List<String> PRODUCTION_SUMMARY_FIELDS = List.of(
            "P1", "P2", "P12", "P15", "P18", "P19", "P23"
    );
    private static final Duration MAX_CONTINUOUS_PRODUCTION_GAP = Duration.ofMinutes(2);

    public void writeTelemetry(String sn, Map<String, Object> points, Map<String, String> tags) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            Point point = Point.measurement("device_telemetry")
                    .time(Instant.now(), WritePrecision.MS);

            // Add tags
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                point.addTag(tag.getKey(), tag.getValue());
            }

            // Add fields (data points)
            for (Map.Entry<String, Object> field : points.entrySet()) {
                Object value = field.getValue();
                if (value instanceof Number) {
                    point.addField(field.getKey(), ((Number) value).doubleValue());
                } else {
                    point.addField(toStringFieldKey(field.getKey()), value.toString());
                }
            }

            writeApi.writePoint(influxBucket, influxOrg, point);
            log.debug("Telemetry written to InfluxDB for SN: {}", sn);

        } catch (Exception e) {
            log.error("Failed to write telemetry to InfluxDB for SN {}: {}", sn, e.getMessage(), e);
        }
    }

    public void writeOnlineStatus(String deviceId, String sn, boolean online) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            Point point = Point.measurement("device_status")
                    .time(Instant.now(), WritePrecision.MS)
                    .addTag("device_id", deviceId)
                    .addTag("sn", sn)
                    .addField("online", online ? 1 : 0);

            writeApi.writePoint(influxBucket, influxOrg, point);
            log.debug("Online status written to InfluxDB for SN: {}, online: {}", sn, online);

        } catch (Exception e) {
            log.error("Failed to write online status to InfluxDB for SN {}: {}", sn, e.getMessage(), e);
        }
    }

    /**
     * Query the latest telemetry data from InfluxDB for a given device SN.
     * Returns a map with "timestamp" and "points" keys.
     * Returns null if no data found.
     */
    public Map<String, Object> queryLatestTelemetry(String deviceId, String legacySn) {
        Map<String, Object> result = queryLatestTelemetryByTag("device_id", deviceId);
        if (result != null) {
            result.put("deviceId", deviceId);
            result.put("sn", legacySn);
        }
        return result;
    }

    private Map<String, Object> queryLatestTelemetryByTag(String tagName, String tagValue) {
        try {
            // Flux query: get the last value for each field, grouped by field name
            String flux = String.format(
                    "from(bucket: \"%s\")\n" +
                    "  |> range(start: -24h)\n" +
                    "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.%s == \"%s\")\n" +
                    "  |> group(columns: [\"_field\"])\n" +
                    "  |> last()",
                    escapeFluxString(influxBucket), tagName, escapeFluxString(tagValue)
            );

            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxOrg);

            if (tables == null || tables.isEmpty()) {
                log.info("No telemetry data found in InfluxDB for {}={}", tagName, tagValue);
                return null;
            }

            Map<String, Object> points = new LinkedHashMap<>();
            Instant latestTime = null;

            for (FluxTable table : tables) {
                List<FluxRecord> records = table.getRecords();
                if (records == null || records.isEmpty()) {
                    continue;
                }
                for (FluxRecord record : records) {
                    String field = record.getField();
                    Object value = record.getValue();
                    if (field != null && value != null) {
                        String pointId = fromStoredFieldKey(field);
                        if (value instanceof Number) {
                            points.putIfAbsent(pointId, ((Number) value).doubleValue());
                        } else {
                            points.put(pointId, value.toString());
                        }
                    }
                    // Track the latest timestamp across all fields
                    if (record.getTime() != null) {
                        if (latestTime == null || record.getTime().isAfter(latestTime)) {
                            latestTime = record.getTime();
                        }
                    }
                }
            }

            if (points.isEmpty()) {
                log.info("No telemetry points found in InfluxDB for {}={}", tagName, tagValue);
                return null;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("points", points);
            result.put("timestamp", latestTime != null ? latestTime.toString() : null);

            log.info("Retrieved {} telemetry points for {}={}", points.size(), tagName, tagValue);
            return result;

        } catch (Exception e) {
            log.error("Failed to query latest telemetry from InfluxDB for {}={}: {}",
                    tagName, tagValue, e.getMessage(), e);
            return null;
        }
    }

    private String toStringFieldKey(String field) {
        return field + "_text";
    }

    private String fromStoredFieldKey(String field) {
        if (isStringFieldKey(field)) {
            return field.substring(0, field.length() - "_text".length());
        }
        return field;
    }

    private boolean isStringFieldKey(String field) {
        return field.endsWith("_text");
    }

    /**
     * Query historical telemetry data from InfluxDB for a given device SN.
     * Returns time-series data grouped by field, suitable for charting.
     *
     * @param sn     device serial number
     * @param range  time range: "1h", "6h", "24h", "7d", "30d"
     * @param fields optional list of field names (e.g., ["P1","P17"]); if null/empty, returns all fields
     * @return Map with keys: sn, range, interval, series (list of {field, data:[{time, value}]})
     */
    public Map<String, Object> queryHistoryTelemetry(
            String deviceId, String legacySn, String range, List<String> fields) {
        return queryHistoryTelemetry(deviceId, legacySn, range, fields, null);
    }

    /**
     * Query historical telemetry data from InfluxDB for a given device SN.
     * Returns time-series data grouped by field, suitable for charting.
     *
     * @param sn       device serial number
     * @param range    time range: "1h", "6h", "24h", "7d", "30d"
     * @param fields   optional list of field names (e.g., ["P1","P17"]); if null/empty, returns all fields
     * @param interval optional aggregation interval; if empty, uses the default interval for the range
     * @return Map with keys: sn, range, interval, series (list of {field, data:[{time, value}]})
     */
    public Map<String, Object> queryHistoryTelemetry(
            String deviceId, String legacySn, String range, List<String> fields, String interval) {
        Map<String, Object> result = queryHistoryTelemetryByTag(
                "device_id", deviceId, range, fields, interval);
        if (result != null) {
            result.put("deviceId", deviceId);
            result.put("sn", legacySn);
        }
        return result;
    }

    private Map<String, Object> queryHistoryTelemetryByTag(
            String tagName, String tagValue, String range, List<String> fields, String interval) {
        try {
            // Parse range to Flux start and aggregation interval
            String fluxStart = parseRangeToFluxStart(range);
            String queryInterval = parseHistoryInterval(range, interval);

            // Build Flux query
            StringBuilder flux = new StringBuilder();
            flux.append(String.format(
                    "from(bucket: \"%s\")\n" +
                    "  |> range(start: %s)\n" +
                    "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.%s == \"%s\")\n",
                    escapeFluxString(influxBucket), fluxStart, tagName, escapeFluxString(tagValue)
            ));

            // Add field filter if specified
            if (fields != null && !fields.isEmpty()) {
                String fieldList = fields.stream()
                        .map(f -> "\"" + f + "\"")
                        .collect(Collectors.joining(", "));
                flux.append(String.format(
                        "  |> filter(fn: (r) => contains(value: r._field, set: [%s]))\n",
                        fieldList
                ));
            }

            // Add aggregation window (mean) to downsample data
            flux.append("  |> filter(fn: (r) => exists r._value)\n");
            flux.append(String.format(
                    "  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)\n",
                    queryInterval
            ));
            flux.append("  |> keep(columns: [\"_time\", \"_field\", \"_value\"])\n");

            // Group by field so each FluxTable corresponds to one field
            flux.append("  |> group(columns: [\"_field\"])");

            log.debug("Flux query for history telemetry: {}", flux);

            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux.toString(), influxOrg);

            List<Map<String, Object>> seriesList = new ArrayList<>();

            if (tables != null) {
                for (FluxTable table : tables) {
                    List<FluxRecord> records = table.getRecords();
                    if (records == null || records.isEmpty()) {
                        continue;
                    }

                    String fieldName = records.get(0).getField();
                    List<Map<String, Object>> dataPoints = new ArrayList<>();

                    for (FluxRecord record : records) {
                        Object value = record.getValue();
                        if (value instanceof Number) {
                            Map<String, Object> point = new LinkedHashMap<>();
                            point.put("time", record.getTime() != null ? record.getTime().toString() : null);
                            point.put("value", ((Number) value).doubleValue());
                            dataPoints.add(point);
                        }
                    }

                    if (!dataPoints.isEmpty()) {
                        Map<String, Object> series = new LinkedHashMap<>();
                        series.put("field", fieldName);
                        series.put("data", dataPoints);
                        seriesList.add(series);
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("range", range);
            result.put("interval", queryInterval);
            result.put("series", seriesList);

            log.info("Retrieved {} field series for {}={}, range={}",
                    seriesList.size(), tagName, tagValue, range);
            return result;

        } catch (Exception e) {
            log.error("Failed to query history telemetry for {}={}: {}",
                    tagName, tagValue, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Returns a compact production-only summary for the controlled assistant.
     *
     * <p>The calculation is always bound to the immutable {@code device_id} tag.
     * It only uses samples whose P23 production state is enabled, so standby
     * pressure and TDS values do not pollute the production assessment.</p>
     */
    public Map<String, Object> queryProductionSummary(
            String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Instant start = startTime.atZone(ZoneId.systemDefault()).toInstant();
            Instant end = endTime.atZone(ZoneId.systemDefault()).toInstant();
            String aggregationInterval = selectProductionAggregationInterval(start, end);
            String fieldList = PRODUCTION_SUMMARY_FIELDS.stream()
                    .map(field -> "\"" + field + "\"")
                    .collect(Collectors.joining(", "));

            String flux = String.format(
                    "from(bucket: \"%s\")\n" +
                            "  |> range(start: %s, stop: %s)\n" +
                            "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.device_id == \"%s\")\n" +
                            "  |> filter(fn: (r) => contains(value: r._field, set: [%s]))\n" +
                            "  |> filter(fn: (r) => exists r._value)\n" +
                            "  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)\n" +
                            "  |> keep(columns: [\"_time\", \"_field\", \"_value\"])\n" +
                            "  |> sort(columns: [\"_time\"])",
                    escapeFluxString(influxBucket), start, end, escapeFluxString(deviceId), fieldList,
                    aggregationInterval);

            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxOrg);
            Map<Instant, Map<String, Double>> valuesByTime = new TreeMap<>();
            if (tables != null) {
                for (FluxTable table : tables) {
                    for (FluxRecord record : table.getRecords()) {
                        if (record.getTime() == null || record.getField() == null
                                || !(record.getValue() instanceof Number value)) {
                            continue;
                        }
                        valuesByTime.computeIfAbsent(record.getTime(), ignored -> new HashMap<>())
                                .put(record.getField(), value.doubleValue());
                    }
                }
            }

            List<ProductionSample> samples = valuesByTime.entrySet().stream()
                    .map(entry -> new ProductionSample(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(ProductionSample::time))
                    .toList();

            Map<String, Object> summary = summarizeProductionSamples(samples);
            summary.put("startTime", startTime.toString());
            summary.put("endTime", endTime.toString());
            summary.put("aggregationInterval", aggregationInterval);
            summary.put("telemetrySamples", samples.size());
            return summary;
        } catch (Exception e) {
            log.error("Failed to query production summary for deviceId={}: {}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> summarizeProductionSamples(List<ProductionSample> samples) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (samples.isEmpty()) {
            summary.put("dataStatus", "NO_DATA");
            summary.put("currentProducing", false);
            summary.put("productionSessions", 0);
            summary.put("productionSamples", 0);
            summary.put("productionDurationMinutes", 0D);
            summary.put("pureWaterLiters", 0D);
            summary.put("wasteWaterLiters", 0D);
            summary.put("wasteWaterRatio", null);
            return summary;
        }

        boolean previousProducing = false;
        Instant previousTime = null;
        Double previousPureTotal = null;
        Double previousWasteTotal = null;
        int productionSessions = 0;
        int productionSamples = 0;
        long productionDurationSeconds = 0;
        double pureWaterLiters = 0D;
        double wasteWaterLiters = 0D;
        MetricAccumulator rawTds = new MetricAccumulator();
        MetricAccumulator pureTds = new MetricAccumulator();
        MetricAccumulator membraneFrontPressure = new MetricAccumulator();
        MetricAccumulator membraneRearPressure = new MetricAccumulator();

        for (ProductionSample sample : samples) {
            boolean producing = isProducing(sample.values().get("P23"));
            Duration gap = previousTime == null ? null : Duration.between(previousTime, sample.time());
            boolean continuous = gap != null && !gap.isNegative() && !gap.isZero()
                    && gap.compareTo(MAX_CONTINUOUS_PRODUCTION_GAP) <= 0;

            if (producing && (!previousProducing || !continuous)) {
                productionSessions++;
            }
            if (producing) {
                productionSamples++;
                if (previousProducing && continuous) {
                    productionDurationSeconds += gap.getSeconds();
                    pureWaterLiters += positiveIncrement(previousPureTotal, sample.values().get("P12"));
                    wasteWaterLiters += positiveIncrement(previousWasteTotal, sample.values().get("P15"));
                }
                rawTds.add(sample.values().get("P1"));
                pureTds.add(sample.values().get("P2"));
                membraneFrontPressure.add(sample.values().get("P18"));
                membraneRearPressure.add(sample.values().get("P19"));
            }

            previousPureTotal = sample.values().get("P12") != null
                    ? sample.values().get("P12") : previousPureTotal;
            previousWasteTotal = sample.values().get("P15") != null
                    ? sample.values().get("P15") : previousWasteTotal;
            previousProducing = producing;
            previousTime = sample.time();
        }

        ProductionSample latest = samples.get(samples.size() - 1);
        summary.put("dataStatus", productionSamples == 0 ? "NO_PRODUCTION" : "OK");
        summary.put("currentProducing", isProducing(latest.values().get("P23")));
        summary.put("lastTelemetryAt", latest.time().toString());
        summary.put("productionSessions", productionSessions);
        summary.put("productionSamples", productionSamples);
        summary.put("productionDurationMinutes", round(productionDurationSeconds / 60D));
        summary.put("pureWaterLiters", round(pureWaterLiters));
        summary.put("wasteWaterLiters", round(wasteWaterLiters));
        summary.put("wasteWaterRatio", pureWaterLiters > 0D ? round(wasteWaterLiters / pureWaterLiters) : null);
        summary.put("averageRawTds", rawTds.average());
        summary.put("averagePureTds", pureTds.average());
        summary.put("averageMembraneFrontPressure", membraneFrontPressure.average());
        summary.put("averageMembraneRearPressure", membraneRearPressure.average());
        return summary;
    }

    private String selectProductionAggregationInterval(Instant start, Instant end) {
        Duration range = Duration.between(start, end);
        if (range.compareTo(Duration.ofDays(1)) <= 0) {
            return "10s";
        }
        if (range.compareTo(Duration.ofDays(7)) <= 0) {
            return "30s";
        }
        return "1m";
    }

    private boolean isProducing(Double value) {
        return value != null && value > 0.5D;
    }

    private double positiveIncrement(Double previous, Double current) {
        if (previous == null || current == null) {
            return 0D;
        }
        double increment = current - previous;
        return increment > 0D ? increment : 0D;
    }

    private double round(double value) {
        return Math.round(value * 1000D) / 1000D;
    }

    private record ProductionSample(Instant time, Map<String, Double> values) {
    }

    private static final class MetricAccumulator {
        private double sum;
        private int count;

        void add(Double value) {
            if (value != null) {
                sum += value;
                count++;
            }
        }

        Double average() {
            return count == 0 ? null : Math.round(sum / count * 1000D) / 1000D;
        }
    }

    private String escapeFluxString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Convert range string ("1h", "6h", "24h", "7d", "30d") to Flux start parameter.
     */
    private String parseRangeToFluxStart(String range) {
        if (range == null || range.isBlank()) {
            return "-24h"; // default
        }
        switch (range.trim().toLowerCase()) {
            case "1h":  return "-1h";
            case "6h":  return "-6h";
            case "24h": return "-24h";
            case "7d":  return "-7d";
            case "30d": return "-30d";
            default:    return "-" + range; // pass through for custom ranges like "12h"
        }
    }

    /**
     * Convert range string to aggregation interval for downsampling.
     * Larger ranges use larger intervals to keep the result set manageable.
     */
    private String parseRangeToInterval(String range) {
        if (range == null || range.isBlank()) {
            return "10m"; // default
        }
        switch (range.trim().toLowerCase()) {
            case "1h":  return "1m";
            case "6h":  return "5m";
            case "24h": return "10m";
            case "7d":  return "2h";
            case "30d": return "12h";
            default:    return "10m";
        }
    }

    private String parseHistoryInterval(String range, String interval) {
        if (interval == null || interval.isBlank() || "auto".equalsIgnoreCase(interval.trim())) {
            return parseRangeToInterval(range);
        }

        String normalized = interval.trim().toLowerCase();
        if (ALLOWED_HISTORY_INTERVALS.contains(normalized)) {
            return normalized;
        }

        log.warn("Unsupported history aggregation interval: {}, fallback to range default", interval);
        return parseRangeToInterval(range);
    }
}
