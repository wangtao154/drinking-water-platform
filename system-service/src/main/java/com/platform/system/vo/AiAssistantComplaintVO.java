package com.platform.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 可在后台审计页查看的 AI 助手投诉或举报记录。 */
@Data
public class AiAssistantComplaintVO {

    private Long id;
    private String complaintNo;
    private Long reporterId;
    private String reporterName;
    private String category;
    private String content;
    private String requestId;
    private String status;
    private String handleReply;
    private Long handlerId;
    private String handlerName;
    private LocalDateTime replyDueAt;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime retentionUntil;
}
