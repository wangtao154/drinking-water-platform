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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoMembranePredictionServiceImpl implements RoMembranePredictionService {

    private static final List<String> RO_FIELDS = List.of("P1", "P2", "P7", "P12", "P15", "P18", "P19", "P23");
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

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
            stats = queryStats(device.getDeviceId(), rangeDays, aggregateEvery);
        } catch (Exception ex) {
            log.warn("[AI] Query RO membrane telemetry failed, sn={}, rangeDays={}, error={}",
                    sn, rangeDays, ex.getMessage());
            stats = emptyStats();
            dataStatus = "INFLUX_ERROR";
        }

        RoProductionDataCalculator.Analysis productionContext = RoProductionDataCalculator.Analysis.empty();
        if ("OK".equals(dataStatus)) {
            try {
                productionContext = queryProductionContext(device.getDeviceId(), rangeDays);
            } catch (Exception ex) {
                log.warn("[AI] Query RO membrane production context failed, sn={}, rangeDays={}, error={}",
                        sn, rangeDays, ex.getMessage());
            }
        }

        int dataPointCount = stats.values().stream().mapToInt(MetricStats::getCount).sum();
        if (dataPointCount == 0 && "OK".equals(dataStatus)) {
            dataStatus = "NO_DATA";
        }

        PredictionCalc localCalc = calculate(stats, rangeDays, ratedPureLiters, dataPointCount, productionContext);
        RuleAdviceVO ruleAdvice = buildRuleAdvice(localCalc, productionContext, dataStatus);
        RoMembranePredictionVO base = buildResponse(device, rangeDays, aggregateEvery, ratedPureLiters,
                dataPointCount, dataStatus, stats, localCalc, productionContext, ruleAdvice, null);

        QwenPrimaryPrediction qwenPrediction = buildQwenPrimaryPrediction(base, localCalc, ratedPureLiters);
        return buildResponse(device, rangeDays, aggregateEvery, ratedPureLiters,
                dataPointCount, dataStatus, stats, qwenPrediction.calc(), productionContext,
                ruleAdvice, qwenPrediction.qwenAdvice());
    }

    private Map<String, MetricStats> queryStats(String deviceId, int rangeDays, String aggregateEvery) {
        String fieldList = RO_FIELDS.stream()
                .map(field -> "\"" + field + "\"")
                .collect(Collectors.joining(", "));
        String flux = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: -%dd)\n" +
                "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.device_id == \"%s\")\n" +
                "  |> filter(fn: (r) => contains(value: r._field, set: [%s]))\n" +
                "  |> filter(fn: (r) => exists r._value)\n" +
                "  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)\n" +
                "  |> keep(columns: [\"_time\", \"_field\", \"_value\"])",
                escapeFluxString(influxBucket), rangeDays, escapeFluxString(deviceId), fieldList, aggregateEvery
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

    private RoProductionDataCalculator.Analysis queryProductionContext(String deviceId, int rangeDays) {
        String flux = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: -%dd)\n" +
                "  |> filter(fn: (r) => r._measurement == \"device_telemetry\" and r.device_id == \"%s\")\n" +
                "  |> filter(fn: (r) => contains(value: r._field, set: [\"P1\", \"P2\", \"P7\", \"P12\", \"P15\", \"P18\", \"P19\", \"P23\"]))\n" +
                "  |> filter(fn: (r) => exists r._value)\n" +
                "  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n" +
                "  |> keep(columns: [\"_time\", \"P1\", \"P2\", \"P7\", \"P12\", \"P15\", \"P18\", \"P19\", \"P23\"])\n" +
                "  |> sort(columns: [\"_time\"])",
                escapeFluxString(influxBucket), rangeDays, escapeFluxString(deviceId)
        );

        List<RoProductionDataCalculator.TelemetrySample> samples = new ArrayList<>();
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxOrg);
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                if (record.getTime() != null) {
                    samples.add(new RoProductionDataCalculator.TelemetrySample(
                            record.getTime(),
                            toDouble(record.getValues().get("P1")),
                            toDouble(record.getValues().get("P2")),
                            toDouble(record.getValues().get("P7")),
                            toDouble(record.getValues().get("P12")),
                            toDouble(record.getValues().get("P15")),
                            toDouble(record.getValues().get("P18")),
                            toDouble(record.getValues().get("P19")),
                            toDouble(record.getValues().get("P23"))
                    ));
                }
            }
        }
        return RoProductionDataCalculator.analyze(samples);
    }

    private PredictionCalc calculate(Map<String, MetricStats> stats,
                                     int rangeDays,
                                     double ratedPureLiters,
                                     int dataPointCount,
                                     RoProductionDataCalculator.Analysis productionContext) {
        Double rawTds = productionContext.productionRawTds();
        Double pureTds = productionContext.productionPureTds();
        Double pureTotal = productionContext.latestPureTotal();
        Double wasteDelta = productionContext.wasteDelta();
        Double pureDelta = productionContext.pureDelta();

        Double desalinationRate = null;
        if (rawTds != null && pureTds != null && rawTds > 0) {
            desalinationRate = Math.max(0D, (rawTds - pureTds) / rawTds * 100D);
        }

        Double wastewaterRatio = null;
        if (wasteDelta != null && pureDelta != null && pureDelta > 0 && wasteDelta >= 0) {
            wastewaterRatio = wasteDelta / pureDelta;
        }

        Double pressureDiff = productionContext.productionPressureDiff();
        boolean waterProducing = productionContext.productionDetected();
        RoProductionDataCalculator.PressureEvaluation pressureEvaluation =
                RoProductionDataCalculator.evaluatePressure(productionContext);
        String pressureAssessment = pressureEvaluation.code();
        String pressureAssessmentMessage = pressureEvaluation.message();

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

        if (pressureEvaluation.foulingSuspected()) {
            score -= 18;
            reasons.add("稳定制水时膜前压力持续上升且纯水流量下降，可能存在RO膜堵塞或水路阻力升高");
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
        Double dailyPureLiters = pureDelta != null
                && pureDelta > 0
                && productionContext.dataCoverageDays() >= 0.25D
                ? pureDelta / productionContext.dataCoverageDays()
                : null;
        Double remainingLiters = pureTotal == null ? null : Math.max(0D, ratedPureLiters - pureTotal);
        Double remainingDays = null;
        if (remainingLiters != null && dailyPureLiters != null && dailyPureLiters > 0) {
            double scoreFactor = score < 60 ? 0.7 : score < 80 ? 0.85 : 1.0;
            remainingDays = remainingLiters / dailyPureLiters * scoreFactor;
        }

        return new PredictionCalc(score, riskLevel, remainingLiters, remainingDays, dailyPureLiters,
                productionContext.productionRawTds(), productionContext.productionPureTds(),
                desalinationRate, wastewaterRatio, pressureDiff, waterProducing,
                pressureAssessment, pressureAssessmentMessage, reasons,
                "LOCAL_RULE", null);
    }

    private RuleAdviceVO buildRuleAdvice(PredictionCalc calc,
                                         RoProductionDataCalculator.Analysis productionContext,
                                         String dataStatus) {
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

        String confidence = "OK".equals(dataStatus) ? productionContext.confidence() : "LOW";
        return RuleAdviceVO.builder()
                .summary("规则评估结果：" + calc.riskLevel() + "，健康分 " + round(calc.healthScore()))
                .reasons(calc.reasons().isEmpty() ? List.of("当前关键指标未触发明显异常") : calc.reasons())
                .recommendedActions(actions)
                .confidence(confidence)
                .build();
    }

    private QwenPrimaryPrediction buildQwenPrimaryPrediction(RoMembranePredictionVO base,
                                                             PredictionCalc localCalc,
                                                             double ratedPureLiters) {
        if (!qwenClient.isConfigured()) {
            String reason = "未配置或未启用 Qwen，已使用本地规则兜底预测";
            return fallbackPrediction(localCalc, "SKIPPED", reason);
        }

        try {
            String prompt = """
                    请作为主预测模型，根据下面的直饮水设备 RO 膜统计数据，直接预测 RO 膜健康分、风险等级、剩余可制纯水量和剩余寿命天数。
                    输出必须是 JSON，不要输出 Markdown，不要增加未要求字段。

                    必须返回字段：
                    {
                      "healthScore": 0-100 的数字,
                      "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                      "estimatedRemainingLiters": 非负数字,
                      "estimatedRemainingDays": 非负数字,
                      "maintenancePriority": "低|中|高|紧急",
                      "confidence": "LOW|MEDIUM|HIGH",
                      "summary": "一句话结论",
                      "recommendedActions": ["建议1", "建议2"],
                      "reasoning": "简要说明判断依据"
                    }

                    判断约束：
                    1. 顶层 healthScore、riskLevel、estimatedRemainingLiters、estimatedRemainingDays 以你的预测为准，本地 ruleAdvice 只是参考和兜底。
                    2. productionPureTds 和 productionRawTds 来自分析周期内全部有效稳定制水会话，已排除待机、制水启动阶段和无效零值。
                    3. membranePressureDiff 仅用于展示工况，绝对压差不能单独作为 RO 膜堵塞依据。
                    4. 只有 pressureFoulingSuspected=true 时，才允许根据压力和流量趋势判断可能堵塞。
                    5. pressureAssessment=BACKPRESSURE_DROP_IGNORED 表示储水桶放空等原因导致膜后背压下降；膜前压力未升高，不得因此降低健康分或提高风险等级。
                    6. 判断堵塞必须同时出现膜前压力上升和纯水流量下降；只有其中一项时只能建议观察，不能直接判堵塞。
                    7. 输入数据已经收敛为制水工况指标，不允许使用待机数据补全缺失的 TDS 或压力。
                    8. waterProducing=true 表示分析周期内检测到制水活动，不等同于接口返回时设备正在制水。
                    9. productionDataConfidence=LOW 时必须降低预测置信度并在结论中说明稳定制水样本不足。
                    10. 剩余升数不能大于 ratedPureLiters 的 1.2 倍，剩余天数应结合 dailyPureLiters 和 dataCoverageDays 做保守估算。

                    数据：
                    %s
                    """.formatted(objectMapper.writeValueAsString(buildQwenInput(base)));
            Optional<QwenAdviceResult> result = qwenClient.generateAdvice(prompt);
            if (result.isEmpty()) {
                String reason = "Qwen 模型未返回有效结果，已使用本地规则兜底预测";
                return fallbackPrediction(localCalc, "MODEL_ERROR", reason);
            }

            QwenAdviceResult advice = result.get();
            String validationError = validateQwenPrediction(advice, ratedPureLiters);
            if (validationError != null) {
                String reason = "Qwen 主预测结果未通过校验：" + validationError + "，已使用本地规则兜底预测";
                return fallbackPrediction(localCalc, "MODEL_ERROR", reason);
            }

            PredictionCalc qwenCalc = new PredictionCalc(
                    clamp(advice.getHealthScore(), 0D, 100D),
                    normalizeRiskLevel(advice.getRiskLevel()),
                    advice.getEstimatedRemainingLiters(),
                    advice.getEstimatedRemainingDays(),
                    localCalc.dailyPureLiters(),
                    localCalc.productionRawTds(),
                    localCalc.productionPureTds(),
                    localCalc.desalinationRate(),
                    localCalc.wastewaterRatio(),
                    localCalc.membranePressureDiff(),
                    localCalc.waterProducing(),
                    localCalc.pressureAssessment(),
                    localCalc.pressureAssessmentMessage(),
                    localCalc.reasons(),
                    "QWEN_PRIMARY",
                    null
            );

            QwenAdviceVO qwenAdvice = QwenAdviceVO.builder()
                    .status("OK")
                    .model(qwenClient.getModel())
                    .healthScore(round(qwenCalc.healthScore()))
                    .summary(advice.getSummary())
                    .riskLevel(qwenCalc.riskLevel())
                    .estimatedRemainingLiters(round(qwenCalc.estimatedRemainingLiters()))
                    .estimatedRemainingDays(round(qwenCalc.estimatedRemainingDays()))
                    .maintenancePriority(advice.getMaintenancePriority())
                    .confidence(advice.getConfidence())
                    .recommendedActions(advice.getRecommendedActions())
                    .reasoning(advice.getReasoning())
                    .build();
            return new QwenPrimaryPrediction(qwenCalc, qwenAdvice);
        } catch (Exception ex) {
            log.warn("[AI] Qwen primary prediction failed: {}", ex.getMessage());
            String reason = "Qwen 主预测调用异常，已使用本地规则兜底预测";
            return fallbackPrediction(localCalc, "MODEL_ERROR", reason);
        }
    }

    private QwenPrimaryPrediction fallbackPrediction(PredictionCalc localCalc, String status, String reason) {
        PredictionCalc fallbackCalc = withPredictionSource(localCalc, "LOCAL_RULE", reason);
        QwenAdviceVO qwenAdvice = QwenAdviceVO.builder()
                .status(status)
                .model(qwenClient.getModel())
                .errorMessage(reason)
                .build();
        return new QwenPrimaryPrediction(fallbackCalc, qwenAdvice);
    }

    private PredictionCalc withPredictionSource(PredictionCalc calc, String source, String fallbackReason) {
        return new PredictionCalc(
                calc.healthScore(),
                calc.riskLevel(),
                calc.estimatedRemainingLiters(),
                calc.estimatedRemainingDays(),
                calc.dailyPureLiters(),
                calc.productionRawTds(),
                calc.productionPureTds(),
                calc.desalinationRate(),
                calc.wastewaterRatio(),
                calc.membranePressureDiff(),
                calc.waterProducing(),
                calc.pressureAssessment(),
                calc.pressureAssessmentMessage(),
                calc.reasons(),
                source,
                fallbackReason
        );
    }

    private String validateQwenPrediction(QwenAdviceResult advice, double ratedPureLiters) {
        if (!isFinite(advice.getHealthScore())) {
            return "healthScore 缺失或不是有效数字";
        }
        if (!RISK_LEVELS.contains(normalizeRiskLevel(advice.getRiskLevel()))) {
            return "riskLevel 必须是 LOW、MEDIUM、HIGH 或 CRITICAL";
        }
        if (!isFinite(advice.getEstimatedRemainingLiters()) || advice.getEstimatedRemainingLiters() < 0) {
            return "estimatedRemainingLiters 缺失或小于 0";
        }
        if (!isFinite(advice.getEstimatedRemainingDays()) || advice.getEstimatedRemainingDays() < 0) {
            return "estimatedRemainingDays 缺失或小于 0";
        }
        double maxRemainingLiters = Math.max(ratedPureLiters * 1.2D, ratedPureLiters + 100D);
        if (advice.getEstimatedRemainingLiters() > maxRemainingLiters) {
            return "estimatedRemainingLiters 超过额定纯水量的合理上限";
        }
        if (advice.getEstimatedRemainingDays() > 36500D) {
            return "estimatedRemainingDays 超过合理上限";
        }
        return null;
    }

    private RoMembranePredictionVO buildResponse(DeviceMetadata device,
                                                 int rangeDays,
                                                 String aggregateEvery,
                                                 double ratedPureLiters,
                                                 int dataPointCount,
                                                 String dataStatus,
                                                 Map<String, MetricStats> stats,
                                                 PredictionCalc calc,
                                                 RoProductionDataCalculator.Analysis productionContext,
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
                .productionSessionCount(productionContext.productionSessionCount())
                .stableProductionSessionCount(productionContext.stableSessionCount())
                .productionSampleCount(productionContext.stableSampleCount())
                .productionDataConfidence(productionContext.confidence())
                .dataCoverageDays(round(productionContext.dataCoverageDays()))
                .accumulatedPureLiters(round(productionContext.latestPureTotal()))
                .observedPureLiters(round(productionContext.pureDelta()))
                .observedWasteLiters(round(productionContext.wasteDelta()))
                .predictionSource(calc.predictionSource())
                .predictionFallbackReason(calc.predictionFallbackReason())
                .healthScore(round(calc.healthScore()))
                .riskLevel(calc.riskLevel())
                .estimatedRemainingLiters(round(calc.estimatedRemainingLiters()))
                .estimatedRemainingDays(round(calc.estimatedRemainingDays()))
                .dailyPureLiters(round(calc.dailyPureLiters()))
                .productionRawTds(round(calc.productionRawTds()))
                .productionPureTds(round(calc.productionPureTds()))
                .desalinationRate(round(calc.desalinationRate()))
                .wastewaterRatio(round(calc.wastewaterRatio()))
                .membranePressureDiff(round(calc.membranePressureDiff()))
                .productionMembraneBefore(round(productionContext.productionMembraneBefore()))
                .productionMembraneAfter(round(productionContext.productionMembraneAfter()))
                .productionPureFlow(round(productionContext.productionPureFlow()))
                .membraneBeforeChangePercent(round(productionContext.membraneBeforeChangePercent()))
                .membraneAfterChangePercent(round(productionContext.membraneAfterChangePercent()))
                .pureFlowChangePercent(round(productionContext.pureFlowChangePercent()))
                .pressureFoulingSuspected("POSSIBLE_FOULING".equals(calc.pressureAssessment()))
                .backpressureDropIgnored("BACKPRESSURE_DROP_IGNORED".equals(calc.pressureAssessment()))
                .waterProducing(calc.waterProducing())
                .pressureAssessment(calc.pressureAssessment())
                .pressureAssessmentMessage(calc.pressureAssessmentMessage())
                .metrics(toMetricVo(stats))
                .ruleAdvice(ruleAdvice)
                .qwenAdvice(qwenAdvice)
                .build();
    }

    private Map<String, Object> buildQwenInput(RoMembranePredictionVO base) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("deviceId", base.getDeviceId());
        input.put("modelName", base.getModelName());
        input.put("rangeDays", base.getRangeDays());
        input.put("ratedPureLiters", base.getRatedPureLiters());
        input.put("dataCoverageDays", base.getDataCoverageDays());
        input.put("productionSessionCount", base.getProductionSessionCount());
        input.put("stableProductionSessionCount", base.getStableProductionSessionCount());
        input.put("productionSampleCount", base.getProductionSampleCount());
        input.put("productionDataConfidence", base.getProductionDataConfidence());
        input.put("waterProducing", base.getWaterProducing());
        input.put("productionRawTds", base.getProductionRawTds());
        input.put("productionPureTds", base.getProductionPureTds());
        input.put("desalinationRate", base.getDesalinationRate());
        input.put("membranePressureDiff", base.getMembranePressureDiff());
        input.put("productionMembraneBefore", base.getProductionMembraneBefore());
        input.put("productionMembraneAfter", base.getProductionMembraneAfter());
        input.put("productionPureFlow", base.getProductionPureFlow());
        input.put("membraneBeforeChangePercent", base.getMembraneBeforeChangePercent());
        input.put("membraneAfterChangePercent", base.getMembraneAfterChangePercent());
        input.put("pureFlowChangePercent", base.getPureFlowChangePercent());
        input.put("pressureFoulingSuspected", base.getPressureFoulingSuspected());
        input.put("backpressureDropIgnored", base.getBackpressureDropIgnored());
        input.put("pressureAssessment", base.getPressureAssessment());
        input.put("accumulatedPureLiters", base.getAccumulatedPureLiters());
        input.put("observedPureLiters", base.getObservedPureLiters());
        input.put("observedWasteLiters", base.getObservedWasteLiters());
        input.put("dailyPureLiters", base.getDailyPureLiters());
        input.put("wastewaterRatio", base.getWastewaterRatio());
        input.put("localRuleAdvice", base.getRuleAdvice());
        return input;
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

    private static String normalizeRiskLevel(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private static boolean isFinite(Double value) {
        return value != null && !value.isNaN() && !value.isInfinite();
    }

    private static double clamp(Double value, double min, double max) {
        if (!isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static Double round(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return Math.round(value * 100D) / 100D;
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
            Double productionRawTds,
            Double productionPureTds,
            Double desalinationRate,
            Double wastewaterRatio,
            Double membranePressureDiff,
            Boolean waterProducing,
            String pressureAssessment,
            String pressureAssessmentMessage,
            List<String> reasons,
            String predictionSource,
            String predictionFallbackReason
    ) {
    }

    private record QwenPrimaryPrediction(
            PredictionCalc calc,
            QwenAdviceVO qwenAdvice
    ) {
    }

}
