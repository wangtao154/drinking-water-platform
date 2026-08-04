package com.platform.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QwenAdviceVO {

    private String status;
    private String model;
    private String summary;
    private String riskLevel;
    private String maintenancePriority;
    private List<String> recommendedActions;
    private String reasoning;
    private String errorMessage;
}
