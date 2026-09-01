package com.platform.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** Safe integrity verification result for the AI audit log. */
@Data
public class AiAssistantAuditIntegrityVO {

    private String status;
    private String message;
    private Long totalRecords;
    private Long checkedRecords;
    private Long legacyUnsealedRecords;
    private Long firstProblemRecordId;
    private LocalDateTime firstProblemCreatedAt;
    private LocalDateTime verifiedAt;
}
