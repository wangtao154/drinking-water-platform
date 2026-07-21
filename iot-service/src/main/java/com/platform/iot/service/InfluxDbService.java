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

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            "10s", "30s", "1m", "5m", "10m", "30m", "1h", "6h", "12h", "1d"
    );

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

    public void writeOnlineStatus(String sn, boolean online) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            Point point = Point.measurement("device_status")
                    .time(Instant.now(), WritePrecision.MS)
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
    public Map<String, Object> queryLatestTelemetry(String sn) {
        try {
            // Flux query: get the last value for each field, grouped by field name
            String flux = String.format(
                    "from(bucket: \"%s\")\n" +
                    "  |> range(start: -24h)\n" +
                    "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.sn == \"%s\")\n" +
                    "  |> group(columns: [\"_field\"])\n" +
                    "  |> last()",
                    influxBucket, sn
            );

            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxOrg);

            if (tables == null || tables.isEmpty()) {
                log.info("No telemetry data found in InfluxDB for SN: {}", sn);
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
                log.info("No telemetry points found in InfluxDB for SN: {}", sn);
                return null;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("points", points);
            result.put("timestamp", latestTime != null ? latestTime.toString() : null);

            log.info("Retrieved {} telemetry points for SN: {}", points.size(), sn);
            return result;

        } catch (Exception e) {
            log.error("Failed to query latest telemetry from InfluxDB for SN {}: {}", sn, e.getMessage(), e);
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
    public Map<String, Object> queryHistoryTelemetry(String sn, String range, List<String> fields) {
        return queryHistoryTelemetry(sn, range, fields, null);
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
    public Map<String, Object> queryHistoryTelemetry(String sn, String range, List<String> fields, String interval) {
        try {
            // Parse range to Flux start and aggregation interval
            String fluxStart = parseRangeToFluxStart(range);
            String queryInterval = parseHistoryInterval(range, interval);

            // Build Flux query
            StringBuilder flux = new StringBuilder();
            flux.append(String.format(
                    "from(bucket: \"%s\")\n" +
                    "  |> range(start: %s)\n" +
                    "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.sn == \"%s\")\n",
                    influxBucket, fluxStart, sn
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
            flux.append(String.format(
                    "  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)\n",
                    queryInterval
            ));

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
            result.put("sn", sn);
            result.put("range", range);
            result.put("interval", queryInterval);
            result.put("series", seriesList);

            log.info("Retrieved {} field series for SN: {}, range: {}", seriesList.size(), sn, range);
            return result;

        } catch (Exception e) {
            log.error("Failed to query history telemetry for SN {}: {}", sn, e.getMessage(), e);
            return null;
        }
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
            case "7d":  return "1h";
            case "30d": return "6h";
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
