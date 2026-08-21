package com.platform.ai.assistant.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminAssistantCitationVO {

    private String id;
    private String title;
    private String updatedAt;
    private List<String> sources;
}
