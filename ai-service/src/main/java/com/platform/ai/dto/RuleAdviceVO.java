package com.platform.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RuleAdviceVO {

    private String summary;
    private List<String> reasons;
    private List<String> recommendedActions;
    private String confidence;
}
