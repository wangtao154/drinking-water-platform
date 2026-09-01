-- AI-08: AI 审计日志完整性保护。
-- 执行前请先完成数据库备份；该脚本可重复执行。

-- MySQL 8.0 does not support ALTER TABLE ... ADD COLUMN IF NOT EXISTS.
-- Use metadata checks so the migration remains repeatable.
SET @ddl = IF(
        (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'ai_assistant_audit_log'
           AND column_name = 'integrity_version') = 0,
        'ALTER TABLE ai_assistant_audit_log ADD COLUMN integrity_version VARCHAR(32) NULL COMMENT ''完整性算法版本''',
        'SELECT 1');
PREPARE ai_audit_stmt FROM @ddl;
EXECUTE ai_audit_stmt;
DEALLOCATE PREPARE ai_audit_stmt;

SET @ddl = IF(
        (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'ai_assistant_audit_log'
           AND column_name = 'previous_hash') = 0,
        'ALTER TABLE ai_assistant_audit_log ADD COLUMN previous_hash CHAR(64) NULL COMMENT ''上一条审计记录哈希''',
        'SELECT 1');
PREPARE ai_audit_stmt FROM @ddl;
EXECUTE ai_audit_stmt;
DEALLOCATE PREPARE ai_audit_stmt;

SET @ddl = IF(
        (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'ai_assistant_audit_log'
           AND column_name = 'record_hash') = 0,
        'ALTER TABLE ai_assistant_audit_log ADD COLUMN record_hash CHAR(64) NULL COMMENT ''本条审计记录哈希''',
        'SELECT 1');
PREPARE ai_audit_stmt FROM @ddl;
EXECUTE ai_audit_stmt;
DEALLOCATE PREPARE ai_audit_stmt;

CREATE TABLE IF NOT EXISTS ai_assistant_audit_chain_state (
    id               TINYINT      NOT NULL COMMENT '固定为1的单行链状态',
    last_log_id      BIGINT       NULL COMMENT '最后一条已封存审计记录ID',
    last_record_hash CHAR(64)     NULL COMMENT '最后一条已封存审计记录哈希',
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                  ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助手审计哈希链状态';

INSERT IGNORE INTO ai_assistant_audit_chain_state (id, last_log_id, last_record_hash)
VALUES (1, NULL, NULL);

SET @ddl = IF(
        (SELECT COUNT(*) FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = 'ai_assistant_audit_log'
           AND index_name = 'idx_ai_audit_record_hash') = 0,
        'CREATE INDEX idx_ai_audit_record_hash ON ai_assistant_audit_log (record_hash)',
        'SELECT 1');
PREPARE ai_audit_stmt FROM @ddl;
EXECUTE ai_audit_stmt;
DEALLOCATE PREPARE ai_audit_stmt;

DROP TRIGGER IF EXISTS trg_ai_audit_log_before_update;
DROP TRIGGER IF EXISTS trg_ai_audit_log_before_delete;

DELIMITER $$
CREATE TRIGGER trg_ai_audit_log_before_update
BEFORE UPDATE ON ai_assistant_audit_log
FOR EACH ROW
BEGIN
    IF NOT (OLD.request_id <=> NEW.request_id)
       OR NOT (OLD.operator_id <=> NEW.operator_id)
       OR NOT (OLD.operator_name_masked <=> NEW.operator_name_masked)
       OR NOT (OLD.model_name <=> NEW.model_name)
       OR NOT (OLD.result_status <=> NEW.result_status)
       OR NOT (OLD.fallback <=> NEW.fallback)
       OR NOT (OLD.tool_names <=> NEW.tool_names)
       OR NOT (OLD.knowledge_document_ids <=> NEW.knowledge_document_ids)
       OR NOT (OLD.question_digest <=> NEW.question_digest)
       OR NOT (OLD.question_summary_masked <=> NEW.question_summary_masked)
       OR NOT (OLD.answer_digest <=> NEW.answer_digest)
       OR NOT (OLD.answer_summary_masked <=> NEW.answer_summary_masked)
       OR NOT (OLD.error_code <=> NEW.error_code)
       OR NOT (OLD.error_summary_masked <=> NEW.error_summary_masked)
       OR NOT (OLD.integrity_version <=> NEW.integrity_version)
       OR NOT (OLD.previous_hash <=> NEW.previous_hash)
       OR NOT (OLD.record_hash <=> NEW.record_hash)
       OR NOT (OLD.created_at <=> NEW.created_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AI审计日志内容不可修改';
    END IF;

    IF NEW.retention_until < OLD.retention_until THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AI审计日志保留期限不得缩短';
    END IF;

    IF OLD.account_cancelled_at IS NOT NULL
       AND NOT (OLD.account_cancelled_at <=> NEW.account_cancelled_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AI审计日志注销时间不可修改';
    END IF;
END$$

CREATE TRIGGER trg_ai_audit_log_before_delete
BEFORE DELETE ON ai_assistant_audit_log
FOR EACH ROW
BEGIN
    IF OLD.retention_until >= NOW(3) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AI审计日志未到最早清理时间，不允许删除';
    END IF;
END$$
DELIMITER ;
