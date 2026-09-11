-- Corrective migration for AI audit hash records created before timestamp
-- canonicalization was aligned with MySQL DATETIME(3) millisecond precision.
-- Substantive audit fields are not changed. Existing integrity metadata is
-- preserved in a backup table and the corrected chain starts at the next log.

CREATE TABLE IF NOT EXISTS ai_assistant_audit_integrity_backup_20260911 (
    id                BIGINT      NOT NULL,
    integrity_version VARCHAR(32) NULL,
    previous_hash     CHAR(64)    NULL,
    record_hash       CHAR(64)    NULL,
    backed_up_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='AI audit integrity metadata backup before millisecond precision correction';

INSERT IGNORE INTO ai_assistant_audit_integrity_backup_20260911
    (id, integrity_version, previous_hash, record_hash)
SELECT id, integrity_version, previous_hash, record_hash
FROM ai_assistant_audit_log;

SET @ai_audit_integrity_cutoff = (
    SELECT COALESCE(MAX(id), 0)
    FROM ai_assistant_audit_integrity_backup_20260911
);

DROP TRIGGER IF EXISTS trg_ai_audit_log_before_update;

UPDATE ai_assistant_audit_log
SET integrity_version = NULL,
    previous_hash = NULL,
    record_hash = NULL
WHERE id <= @ai_audit_integrity_cutoff;

UPDATE ai_assistant_audit_chain_state
SET last_log_id = NULL,
    last_record_hash = NULL,
    updated_at = NOW(3)
WHERE id = 1
  AND (last_log_id IS NULL OR last_log_id <= @ai_audit_integrity_cutoff);

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
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AI audit log is append-only';
    END IF;
    IF NEW.retention_until < OLD.retention_until THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AI audit retention cannot be reduced';
    END IF;
    IF OLD.account_cancelled_at IS NOT NULL
       AND NOT (OLD.account_cancelled_at <=> NEW.account_cancelled_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AI audit cancellation hold cannot be changed';
    END IF;
END$$
DELIMITER ;
