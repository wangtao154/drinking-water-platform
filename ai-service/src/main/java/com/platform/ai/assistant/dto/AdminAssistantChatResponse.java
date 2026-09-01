package com.platform.ai.assistant.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminAssistantChatResponse {

    private String requestId;
    private String answer;
    private String model;
    private boolean fallback;
    private String notice;
    private List<AdminAssistantCitationVO> citations;
    private List<AdminAssistantDataSourceVO> dataSources;
    /**
     * Conversation identifier. Pass it back on the next message to keep context.
     */
    private String conversationId;
}
