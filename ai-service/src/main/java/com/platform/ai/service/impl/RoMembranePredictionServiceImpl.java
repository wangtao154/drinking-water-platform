package com.platform.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.platform.ai.client.QwenClient;
import com.platform.ai.config.RoPredictionProperties;
import com.platform.ai.dto.MetricStatsVO;
import com.platform.ai.dto.QwenAdviceResult;
import com.platform.ai.dto.QwenAdviceVO;
import com.platform.ai.dto.RoMembranePredictionRequest;
import com.platform.ai.dto.RoMembranePredictionVO;
import com.platform.ai.dto.RuleAdviceVO;
import com.platform.ai.model.DeviceMetadata;
import com.platform.ai.model.MetricStats;
import com.platform.ai.repository.DeviceMetadataRepository;
import com.platform.ai.service.RoMembranePredictionService;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoMembranePredictionServiceImpl implements RoMembranePredictionService {

    private static final List<String> RO_FIELDS = List.of("P1", "P2", "P7", "P12", "P15", "P18", "P19", "P23");
    private static final double PRODUCTION_FLOW_THRESHOLD = 0.001D;
    private static final double PRODUCTION_VOLUME_DELTA_THRESHOLD = 0.01D;
    private static final long PRODUCTION_SESSION_GAP_SECONDS = 300L;

    private static final Map<String, String> FIELD_NAMES = Map.of(
            "P1", "原水TDS",
            "P2", "纯水TDS",
            "P7", "纯水瞬时流量",
            "P12", "纯水累计流量",
            "P15", "废水累计流量",
            "P18", "膜前压力",
            "P19", "膜后压力",
            "P23", "制水状态"
    );

    private static final Map<String, String> FIELD_UNITS = Map.of(
            "P1", "PPM",
            "P2", "PPM",
            "P7", "L/min",
            "P12", "L",
            "P15", "L",
            "P18", "bar",
            "P19", "bar",
            "P23", ""
    );

    private final DeviceMetadataRepository deviceMetadataRepository;
    private final InfluxDBClient influxDBClient;
    private final RoPredictionProperties properties;
    private final QwenClient qwenClient;
    private final ObjectMapper objectMapper;

    @Value("${influxdb.org:platform}")
    private String influxOrg;

    @Value("${influxdb.bucket:drinking_water}")
    private String influxBucket;

    public RoMembranePredictionServiceImpl(DeviceMetadataRepository deviceMetadataRepository,
                                           InfluxDBClient influxDBClient,
                                           RoPredictionProperties properties,
                                           QwenClient qwenClient,
                                           ObjectMapper objectMapper) {
        this.deviceMetadataRepository = deviceMetadataRepository;
        this.influxDBClient = influxDBClient;
        this.properties = properties;
        this.qwenClient = qwenClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public RoMembranePredictionVO predict(RoMembranePredictionRequest request) {
        String sn = request.getSn().trim();
        int rangeDays = request.getRangeDays() == null ? properties.getDefaultRangeDays() : request.getRangeDays();
        double ratedPureLiters = request.getRatedPureLiters() == null
                ? properties.getDefaultRatedPureLiters()
                : request.getRatedPureLiters();
        String aggregateEvery = StringUtils.hasText(request.getAggregateEvery())
                ? request.getAggregateEvery()
                : properties.getDefaultAggregateEvery();

        DeviceMetadata device = deviceMetadataRepository.findBySn(sn)
                .orElseThrow(() -> new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在或已删除: " + sn));

        String dataStatus = "OK";
        Map<String, MetricStats> stats;
        try {
            stats = queryStats(sn, rangeDays, aggregateEvery);
        } catch (Exception ex) {
            log.warn("[AI] Query RO membrane telemetry failed, sn={}, rangeDays={}, error={}",
                    sn, rangeDays, ex.getMessage());
            stats = emptyStats();
            dataStatus = "INFLUX_ERROR";
        }

        ProductionContext productionContext = ProductionContext.empty();
        if ("OK".equals(dataStatus)) {
            try {
                productionContext = queryProductionContext(sn, rangeDays);
            } catch (Exception ex) {
                log.warn("[AI] Query RO membrane production context failed, sn={}, rangeDays={}, error={}",
                        sn, rangeDays, ex.getMessage());
            }
        }

        int dataPointCount = stats.values().stream().mapToInt(MetricStats::getCount).sum();
        if (dataPointCount == 0 && "OK".equals(dataStatus)) {
            dataStatus = "NO_DATA";
        }

        PredictionCalc calc = calculate(stats, rangeDays, ratedPureLiters, dataPointCount, productionContext);
        RuleAdviceVO ruleAdvice = buildRuleAdvice(calc, dataPointCount, dataStatus);
        RoMembranePredictionVO base = buildResponse(device, rangeDays, aggregateEvery, ratedPureLiters,
                dataPointCount, dataStatus, stats, calc, ruleAdvice, null);

        QwenAdviceVO qwenAdvice = buildQwenAdvice(base);
        return buildResponse(device, rangeDays, aggregateEvery, ratedPureLiters,
                dataPointCount, dataStatus, stats, calc, ruleAdvice, qwenAdvice);
    }

    private Map<String, MetricStats> queryStats(String sn, int rangeDays, String aggregateEvery) {
        String fieldList = RO_FIELDS.stream()
                .map(field -> "\"" + field + "\"")
                .collect(Collectors.joining(", "));
        String flux = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: -%dd)\n" +
                "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.sn == \"%s\")\n" +
                "  |> filter(fn: (r) => contains(value: r._field, set: [%s]))\n" +
                "  |> filter(fn: (r) => exists r._value)\n" +
                "  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)\n" +
                "  |> keep(columns: [\"_time\", \"_field\", \"_value\"])",
                escapeFluxString(influxBucket), rangeDays, escapeFluxString(sn), fieldList, aggregateEvery
        );

        Map<String, MetricStats> stats = emptyStats();
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxOrg);
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String field = record.getField();
                if (!stats.containsKey(field)) {
                    continue;
                }
                Double value = toDouble(record.getValue());
                if (value != null && !value.isNaN() && !value.isInfinite()) {
                    stats.get(field).add(value);
                }
            }
        }
        return stats;
    }

    private ProductionContext queryProductionContext(String sn, int rangeDays) {
        String flux = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: -%dd)\n" +
                "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.sn == \"%s\")\n" +
                "  |> filter(fn: (r) => contains(value: r._field, set: [\"P7\", \"P12\", \"P18\", \"P19\", \"P23\"]))\n" +
                "  |> filter(fn: (r) => exists r._value)\n" +
                "  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n" +
                "  |> keep(columns: [\"_time\", \"P7\", \"P12\", \"P18\", \"P19\", \"P23\"])\n" +
                "  |> sort(columns: [\"_time\"])",
                escapeFluxString(influxBucket), rangeDays, escapeFluxString(sn)
        );

        boolean productionDetected = false;
        int productionWindowCount = 0;
        int productionStatusWindowCount = 0;
        boolean hasProductionStatusData = false;
        Double previousPureTotal = null;
        Double productionMembraneBefore = null;
        Double productionMembraneAfter = null;
        Instant previousProductionAt = null;
        List<Double> latestProductionBeforeValues = new ArrayList<>();
        List<Double> latestProductionAfterValues = new ArrayList<>();
        String evidence = null;

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxOrg);
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Double pureInstantFlow = toDouble(record.getValues().get("P7"));
                Double pureTotal = toDouble(record.getValues().get("P12"));
                Double membraneBefore = toDouble(record.getValues().get("P18"));
                Double membraneAfter = toDouble(record.getValues().get("P19"));
                Double productionStatus = toDouble(record.getValues().get("P23"));
                Instant recordTime = record.getTime();

                boolean statusActive = productionStatus != null && productionStatus >= 0.5D;
                boolean flowActive = pureInstantFlow != null && pureInstantFlow > PRODUCTION_FLOW_THRESHOLD;
                boolean totalIncreased = previousPureTotal != null
                        && pureTotal != null
                        && pureTotal - previousPureTotal > PRODUCTION_VOLUME_DELTA_THRESHOLD;

                if (productionStatus != null) {
                    hasProductionStatusData = true;
                }

                boolean fallbackProduction = !hasProductionStatusData && (flowActive || totalIncreased);
                if (statusActive || fallbackProduction) {
                    productionDetected = true;
                    productionWindowCount++;
                    evidence = statusActive ? "P23_STATUS" : flowActive ? "FLOW" : "TOTAL_DELTA";
                    if (statusActive) {
                        productionStatusWindowCount++;
                    }
                    if (statusActive && membraneBefore != null && membraneAfter != null) {
                        if (previousProductionAt != null
                                && recordTime != null
                                && recordTime.isAfter(previousProductionAt.plusSeconds(PRODUCTION_SESSION_GAP_SECONDS))) {
                            latestProductionBeforeValues.clear();
                            latestProductionAfterValues.clear();
                        }
                        latestProductionBeforeValues.add(membraneBefore);
                        latestProductionAfterValues.add(membraneAfter);
                        productionMembraneBefore = median(latestProductionBeforeValues);
                        productionMembraneAfter = median(latestProductionAfterValues);
                    } else if (fallbackProduction && membraneBefore != null && membraneAfter != null) {
                        productionMembraneBefore = membraneBefore;
                        productionMembraneAfter = membraneAfter;
                    }
                }

                if (statusActive && recordTime != null) {
                    previousProductionAt = recordTime;
                }

                if (pureTotal != null) {
                    previousPureTotal = pureTotal;
                }
            }
        }

        return new ProductionContext(productionDetected, productionWindowCount,
                productionStatusWindowCount, hasProductionStatusData,
                productionMembraneBefore, productionMembraneAfter, evidence, "raw");
    }

    private PredictionCalc calculate(Map<String, MetricStats> stats,
                                     int rangeDays,
                                     double ratedPureLiters,
                                     int dataPointCount,
                                     ProductionContext productionContext) {
        Double rawTds = last(stats, "P1");
        Double pureTds = last(stats, "P2");
        Double pureTotal = last(stats, "P12");
        Double wasteDelta = delta(stats, "P15");
        Double pureDelta = delta(stats, "P12");
        Double membraneBefore = last(stats, "P18");
        Double membraneAfter = last(stats, "P19");

        Double desalinationRate = null;
        if (rawTds != null && pureTds != null && rawTds > 0) {
            desalinationRate = Math.max(0D, (rawTds - pureTds) / rawTds * 100D);
        }

        Double wastewaterRatio = null;
        if (wasteDelta != null && pureDelta != null && pureDelta > 0 && wasteDelta >= 0) {
            wastewaterRatio = wasteDelta / pureDelta;
        }

        Double pressureDiff = null;
        if (productionContext.productionMembraneBefore() != null && productionContext.productionMembraneAfter() != null) {
            membraneBefore = productionContext.productionMembraneBefore();
            membraneAfter = productionContext.productionMembraneAfter();
            pressureDiff = Math.abs(membraneBefore - membraneAfter);
        } else if (membraneBefore != null && membraneAfter != null) {
            pressureDiff = Math.abs(membraneBefore - membraneAfter);
        }
        boolean waterProducing = productionContext.productionDetected();
        String pressureAssessment = pressureDiff == null
                ? "UNKNOWN"
                : productionContext.hasProductionPressure()
                ? "PRODUCING_EVALUATED"
                : waterProducing ? "PRODUCING_PRESSURE_MISSING" : "IDLE_IGNORED";
        String pressureAssessmentMessage = switch (pressureAssessment) {
            case "PRODUCING_EVALUATED" -> "分析周期内检测到制水，并已取最近一次制水窗口的膜前膜后压力参与RO膜寿命判断";
            case "PRODUCING_PRESSURE_MISSING" -> "分析周期内检测到制水，但制水窗口压力数据不足，最新压差仅展示不参与扣分";
            case "IDLE_IGNORED" -> "分析周期内未检测到制水活动，非制水状态下膜后压力可能高于膜前压力，压差仅展示不参与扣分";
            default -> "压力数据不足，暂不评价膜前膜后压差";
        };

        double score = dataPointCount == 0 ? 50D : 100D;
        List<String> reasons = new ArrayList<>();

        if (desalinationRate != null) {
            if (desalinationRate < 85) {
                score -= 35;
                reasons.add("脱盐率低于85%，RO膜衰减风险高");
            } else if (desalinationRate < 90) {
                score -= 22;
                reasons.add("脱盐率低于90%，建议重点观察纯水TDS变化");
            } else if (desalinationRate < 95) {
                score -= 10;
                reasons.add("脱盐率低于95%，RO膜性能开始下降");
            }
        } else {
            score -= 8;
            reasons.add("原水TDS或纯水TDS数据不足，脱盐率无法计算");
        }

        if (pureTds != null) {
            if (pureTds > 60) {
                score -= 30;
                reasons.add("纯水TDS高于60PPM，出水水质风险较高");
            } else if (pureTds > 30) {
                score -= 15;
                reasons.add("纯水TDS高于30PPM，建议复核RO膜和后置滤芯");
            }
        }

        if (pressureDiff != null && productionContext.hasProductionPressure()) {
            if (pressureDiff > 4) {
                score -= 18;
                reasons.add("膜前膜后压差偏大，可能存在RO膜堵塞或水路阻力升高");
            } else if (pressureDiff > 2) {
                score -= 8;
                reasons.add("膜前膜后压差略高，建议持续观察压力走势");
            }
        }

        if (wastewaterRatio != null) {
            if (wastewaterRatio > 4 || wastewaterRatio < 0.4) {
                score -= 12;
                reasons.add("废水/纯水累计流量比例异常，建议检查冲洗阀、浓水阀和流量计");
            } else if (wastewaterRatio > 3) {
                score -= 6;
                reasons.add("废水比例偏高，RO膜或水路可能存在效率下降");
            }
        }

        Double usedRatio = null;
        if (pureTotal != null && ratedPureLiters > 0) {
            usedRatio = Math.max(0D, pureTotal / ratedPureLiters);
            if (usedRatio > 1) {
                score -= 20;
                reasons.add("累计纯水量已超过额定参考寿命");
            } else if (usedRatio > 0.8) {
                score -= 10;
                reasons.add("累计纯水量已超过额定参考寿命的80%");
            }
        }

        score = Math.max(0D, Math.min(100D, score));
        String riskLevel = riskLevel(score);
        Double dailyPureLiters = pureDelta != null && pureDelta > 0 ? pureDelta / Math.max(1, rangeDays) : null;
        Double remainingLiters = pureTotal == null ? null : Math.max(0D, ratedPureLiters - pureTotal);
        Double remainingDays = null;
        if (remainingLiters != null && dailyPureLiters != null && dailyPureLiters > 0) {
            double scoreFactor = score < 60 ? 0.7 : score < 80 ? 0.85 : 1.0;
            remainingDays = remainingLiters / dailyPureLiters * scoreFactor;
        }

        return new PredictionCalc(score, riskLevel, remainingLiters, remainingDays, dailyPureLiters,
                desalinationRate, wastewaterRatio, pressureDiff, waterProducing,
                pressureAssessment, pressureAssessmentMessage, reasons);
    }

    private RuleAdviceVO buildRuleAdvice(PredictionCalc calc, int dataPointCount, String dataStatus) {
        List<String> actions = new ArrayList<>();
        if ("NO_DATA".equals(dataStatus) || "INFLUX_ERROR".equals(dataStatus)) {
            actions.add("先确认设备历史数据是否正常上报到InfluxDB");
        }
        if ("HIGH".equals(calc.riskLevel()) || "CRITICAL".equals(calc.riskLevel())) {
            actions.add("安排运维人员现场检测RO膜前后压力、纯水TDS和废水比例");
            actions.add("准备RO膜备件，现场复核后决定是否更换");
        } else if ("MEDIUM".equals(calc.riskLevel())) {
            actions.add("下次巡检时复测纯水TDS，并观察压差趋势");
        } else {
            actions.add("继续按当前周期观察，暂不需要立即更换RO膜");
        }

        String confidence = dataPointCount >= 120 ? "HIGH" : dataPointCount >= 24 ? "MEDIUM" : "LOW";
        return RuleAdviceVO.builder()
                .summary("规则评估结果：" + calc.riskLevel() + "，健康分 " + round(calc.healthScore()))
                .reasons(calc.reasons().isEmpty() ? List.of("当前关键指标未触发明显异常") : calc.reasons())
                .recommendedActions(actions)
                .confidence(confidence)
                .build();
    }

    private QwenAdviceVO buildQwenAdvice(RoMembranePredictionVO base) {
        if (!qwenClient.isConfigured()) {
            return QwenAdviceVO.builder()
                    .status("SKIPPED")
                    .model(qwenClient.getModel())
                    .errorMessage("未配置AI_QWEN_API_KEY，已使用本地规则预测")
                    .build();
        }

        try {
            String prompt = """
                    请根据下面的直饮水设备RO膜统计数据预测RO膜使用寿命，并给出运维建议。
                    输出必须是JSON。
                    判断膜前膜后压差时必须遵守：
                    1. 只有 pressureAssessment=PRODUCING_EVALUATED 时，membranePressureDiff 才用于判断RO膜堵塞或压力异常。
                    2. waterProducing=true 表示分析周期内检测到制水活动，不等同于接口返回时设备正在制水。
                    3. pressureAssessment=IDLE_IGNORED 或 PRODUCING_PRESSURE_MISSING 时，膜后压力高于膜前压力通常属于停机/非制水状态下的管路残压或储水压力现象，不要作为RO膜异常。
                    4. P18/P19 的 metrics 是分析周期统计值，可能包含非制水状态数据；不要自行用 P18/P19 的平均值、最新值或差值推断压差异常，应以 membranePressureDiff 与 pressureAssessment 为准。

                    数据：
                    %s
                    """.formatted(objectMapper.writeValueAsString(base));
            Optional<QwenAdviceResult> result = qwenClient.generateAdvice(prompt);
            if (result.isEmpty()) {
                return QwenAdviceVO.builder()
                        .status("MODEL_ERROR")
                        .model(qwenClient.getModel())
                        .errorMessage("Qwen模型调用失败，已使用本地规则预测")
                        .build();
            }
            QwenAdviceResult advice = result.get();
            return QwenAdviceVO.builder()
                    .status("OK")
                    .model(qwenClient.getModel())
                    .summary(advice.getSummary())
                    .riskLevel(advice.getRiskLevel())
                    .maintenancePriority(advice.getMaintenancePriority())
                    .recommendedActions(advice.getRecommendedActions())
                    .reasoning(advice.getReasoning())
                    .build();
        } catch (Exception ex) {
            log.warn("[AI] Build Qwen prompt failed: {}", ex.getMessage());
            return QwenAdviceVO.builder()
                    .status("MODEL_ERROR")
                    .model(qwenClient.getModel())
                    .errorMessage("AI建议生成失败，已使用本地规则预测")
                    .build();
        }
    }

    private RoMembranePredictionVO buildResponse(DeviceMetadata device,
                                                 int rangeDays,
                                                 String aggregateEvery,
                                                 double ratedPureLiters,
                                                 int dataPointCount,
                                                 String dataStatus,
                                                 Map<String, MetricStats> stats,
                                                 PredictionCalc calc,
                                                 RuleAdviceVO ruleAdvice,
                                                 QwenAdviceVO qwenAdvice) {
        return RoMembranePredictionVO.builder()
                .sn(device.getSn())
                .deviceId(device.getDeviceId())
                .modelName(device.getModelName())
                .online(device.getOnline())
                .lifecycleStatus(device.getLifecycleStatus())
                .activatedAt(device.getActivatedAt())
                .rangeDays(rangeDays)
                .aggregateEvery(aggregateEvery)
                .ratedPureLiters(round(ratedPureLiters))
                .dataPointCount(dataPointCount)
                .dataStatus(dataStatus)
                .healthScore(round(calc.healthScore()))
                .riskLevel(calc.riskLevel())
                .estimatedRemainingLiters(round(calc.estimatedRemainingLiters()))
                .estimatedRemainingDays(round(calc.estimatedRemainingDays()))
                .dailyPureLiters(round(calc.dailyPureLiters()))
                .desalinationRate(round(calc.desalinationRate()))
                .wastewaterRatio(round(calc.wastewaterRatio()))
                .membranePressureDiff(round(calc.membranePressureDiff()))
                .waterProducing(calc.waterProducing())
                .pressureAssessment(calc.pressureAssessment())
                .pressureAssessmentMessage(calc.pressureAssessmentMessage())
                .metrics(toMetricVo(stats))
                .ruleAdvice(ruleAdvice)
                .qwenAdvice(qwenAdvice)
                .build();
    }

    private Map<String, MetricStatsVO> toMetricVo(Map<String, MetricStats> stats) {
        Map<String, MetricStatsVO> result = new LinkedHashMap<>();
        for (Map.Entry<String, MetricStats> entry : stats.entrySet()) {
            MetricStats stat = entry.getValue();
            result.put(entry.getKey(), MetricStatsVO.builder()
                    .field(entry.getKey())
                    .name(FIELD_NAMES.get(entry.getKey()))
                    .unit(FIELD_UNITS.get(entry.getKey()))
                    .count(stat.getCount())
                    .first(round(stat.getFirst()))
                    .last(round(stat.getLast()))
                    .min(round(stat.getMin()))
                    .max(round(stat.getMax()))
                    .avg(round(stat.getAvg()))
                    .delta(round(stat.getDelta()))
                    .build());
        }
        return result;
    }

    private Map<String, MetricStats> emptyStats() {
        Map<String, MetricStats> stats = new LinkedHashMap<>();
        for (String field : RO_FIELDS) {
            stats.put(field, new MetricStats(field));
        }
        return stats;
    }

    private static Double last(Map<String, MetricStats> stats, String field) {
        MetricStats stat = stats.get(field);
        return stat == null ? null : stat.getLast();
    }

    private static Double delta(Map<String, MetricStats> stats, String field) {
        MetricStats stat = stats.get(field);
        return stat == null ? null : stat.getDelta();
    }

    private static String activityAggregateEvery(int rangeDays) {
        if (rangeDays <= 7) {
            return "1m";
        }
        if (rangeDays <= 30) {
            return "5m";
        }
        return "10m";
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String riskLevel(double score) {
        if (score < 45) {
            return "CRITICAL";
        }
        if (score < 65) {
            return "HIGH";
        }
        if (score < 80) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static Double round(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return Math.round(value * 100D) / 100D;
    }

    private static Double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2D;
    }

    private static String escapeFluxString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record PredictionCalc(
            double healthScore,
            String riskLevel,
            Double estimatedRemainingLiters,
            Double estimatedRemainingDays,
            Double dailyPureLiters,
            Double desalinationRate,
            Double wastewaterRatio,
            Double membranePressureDiff,
            Boolean waterProducing,
            String pressureAssessment,
            String pressureAssessmentMessage,
            List<String> reasons
    ) {
    }

    private record ProductionContext(
            boolean productionDetected,
            int productionWindowCount,
            int productionStatusWindowCount,
            boolean hasProductionStatusData,
            Double productionMembraneBefore,
            Double productionMembraneAfter,
            String evidence,
            String activityWindow
    ) {
        private static ProductionContext empty() {
            return new ProductionContext(false, 0, 0, false, null, null, null, null);
        }

        private boolean hasProductionPressure() {
            return productionMembraneBefore != null && productionMembraneAfter != null
                    && (!hasProductionStatusData || productionStatusWindowCount > 0);
        }
    }
}
