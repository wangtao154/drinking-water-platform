-- Scan water outcome inbox. Existing orders and refund records are not rewritten.
CREATE TABLE IF NOT EXISTS water_scan_inbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sn VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    message_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    payload TEXT NOT NULL,
    fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    process_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    process_note VARCHAR(255) NULL,
    received_at DATETIME(3) NOT NULL,
    next_attempt_at DATETIME(3) NOT NULL,
    processed_at DATETIME(3) NULL,
    UNIQUE KEY uk_scan_fingerprint (fingerprint),
    KEY idx_scan_pending (process_status, next_attempt_at, id),
    KEY idx_scan_command (sn, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND column_name = 'command_message_id') = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN command_message_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND column_name = 'stop_message_id') = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN stop_message_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND column_name = 'actual_ml') = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN actual_ml BIGINT NULL', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND column_name = 'actual_estimated') = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN actual_estimated TINYINT(1) NULL', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND column_name = 'result_event') = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN result_event VARCHAR(20) NULL', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND column_name = 'result_reason') = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN result_reason VARCHAR(40) NULL', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND column_name = 'result_received_at') = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN result_received_at DATETIME(3) NULL', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND index_name = 'uk_command_message_id') = 0,
    'ALTER TABLE water_dispense_order ADD UNIQUE KEY uk_command_message_id (command_message_id)', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'water_dispense_order' AND index_name = 'uk_stop_message_id') = 0,
    'ALTER TABLE water_dispense_order ADD UNIQUE KEY uk_stop_message_id (stop_message_id)', 'SELECT 1');
PREPARE scan_stmt FROM @ddl;
EXECUTE scan_stmt;
DEALLOCATE PREPARE scan_stmt;
