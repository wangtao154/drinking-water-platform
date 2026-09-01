-- AI 助手合规审计日志。
-- 仅保存调用元数据及脱敏摘要，不保存完整问题、完整回答或原始敏感数据。
CREATE TABLE IF NOT EXISTS ai_assistant_audit_log (
    id                          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    request_id                  VARCHAR(64)   NOT NULL                COMMENT '一次对话请求唯一标识',
    operator_id                 BIGINT        NOT NULL                COMMENT '后台调用账户ID',
    operator_name_masked        VARCHAR(64)                           COMMENT '调用人脱敏名称快照',
    model_name                  VARCHAR(128)                          COMMENT '实际调用模型，降级时为空',
    result_status               VARCHAR(24)   NOT NULL                COMMENT 'SUCCESS/FALLBACK/REJECTED/ERROR',
    fallback                    TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否降级回答',
    tool_names                  VARCHAR(512)                          COMMENT '受控只读工具名称列表',
    knowledge_document_ids      VARCHAR(512)                          COMMENT '知识库文档ID列表',
    question_digest             CHAR(64)                              COMMENT '问题SHA-256摘要',
    question_summary_masked     VARCHAR(256)                          COMMENT '问题脱敏摘要',
    answer_digest               CHAR(64)                              COMMENT '回答SHA-256摘要',
    answer_summary_masked       VARCHAR(256)                          COMMENT '回答脱敏摘要',
    error_code                  VARCHAR(64)                           COMMENT '失败代码',
    error_summary_masked        VARCHAR(256)                          COMMENT '失败原因脱敏摘要',
    account_cancelled_at        DATETIME(3)                           COMMENT '账户注销/删除时间',
    retention_until             DATETIME(3)   NOT NULL                COMMENT '最早清理时间',
    created_at                  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_audit_request_id (request_id),
    KEY idx_ai_audit_operator_id (operator_id),
    KEY idx_ai_audit_retention_until (retention_until),
    KEY idx_ai_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助手合规审计日志（仅元数据与脱敏摘要）';
