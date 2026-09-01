package com.platform.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** Safe-to-display fields from the minimal AI compliance audit record. */
@Data
public class AiAssistantAuditLogVO {

    private Long id;
    private String requestId;
    private Long operatorId;
    private String operatorNameMasked;
    private String modelName;
    private String resultStatus;
    private Boolean fallback;
    private String toolNames;
    private String knowledgeDocumentIds;
    private String questionSummaryMasked;
    private String answerSummaryMasked;
    private String errorCode;
    private String errorSummaryMasked;
    private LocalDateTime accountCancelledAt;
    private LocalDateTime retentionUntil;
    private LocalDateTime createdAt;
}
