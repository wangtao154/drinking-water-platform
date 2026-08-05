package com.platform.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QwenAdviceResult {

    private Double healthScore;
    private String summary;
    private String riskLevel;
    private Double estimatedRemainingLiters;
    private Double estimatedRemainingDays;
    private String maintenancePriority;
    private String confidence;
    private List<String> recommendedActions;
    private String reasoning;
}
