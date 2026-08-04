package com.platform.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QwenAdviceResult {

    private String summary;
    private String riskLevel;
    private String maintenancePriority;
    private List<String> recommendedActions;
    private String reasoning;
}
