package com.platform.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RoMembranePredictionVO {

    private String sn;
    private String deviceId;
    private String modelName;
    private Boolean online;
    private String lifecycleStatus;
    private LocalDateTime activatedAt;
    private Integer rangeDays;
    private String aggregateEvery;
    private Double ratedPureLiters;
    private Integer dataPointCount;
    private String dataStatus;
    private Integer productionSessionCount;
    private Integer stableProductionSessionCount;
    private Integer productionSampleCount;
    private String productionDataConfidence;
    private Double dataCoverageDays;
    private Double accumulatedPureLiters;
    private Double observedPureLiters;
    private Double observedWasteLiters;
    private String predictionSource;
    private String predictionFallbackReason;

    private Double healthScore;
    private String riskLevel;
    private Double estimatedRemainingLiters;
    private Double estimatedRemainingDays;
    private Double dailyPureLiters;
    private Double productionRawTds;
    private Double productionPureTds;
    private Double desalinationRate;
    private Double wastewaterRatio;
    private Double membranePressureDiff;
    private Double productionMembraneBefore;
    private Double productionMembraneAfter;
    private Double productionPureFlow;
    private Double membraneBeforeChangePercent;
    private Double membraneAfterChangePercent;
    private Double pureFlowChangePercent;
    private Boolean pressureFoulingSuspected;
    private Boolean backpressureDropIgnored;
    private Boolean waterProducing;
    private String pressureAssessment;
    private String pressureAssessmentMessage;

    private Map<String, MetricStatsVO> metrics;
    private RuleAdviceVO ruleAdvice;
    private QwenAdviceVO qwenAdvice;
}
