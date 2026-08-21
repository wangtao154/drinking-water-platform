package com.platform.ai.assistant.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * A minimized, permission-checked snapshot used to answer a data question.
 */
@Data
@Builder
public class AdminAssistantDataSourceVO {

    private String tool;
    private String title;
    private String status;
    private String summary;
    private String dataRange;
    private String queriedAt;
    private Map<String, Object> facts;
}
