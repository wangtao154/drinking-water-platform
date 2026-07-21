-- 修复 worker.phone 与逻辑删除不兼容的问题：
-- deleted=0 的运维人员手机号唯一；已逻辑删除记录不再占用手机号。

DROP PROCEDURE IF EXISTS fix_worker_phone_active_unique;
DELIMITER //
CREATE PROCEDURE fix_worker_phone_active_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'worker'
          AND COLUMN_NAME = 'active_flag'
    ) THEN
        ALTER TABLE worker
            ADD COLUMN active_flag TINYINT(1)
                GENERATED ALWAYS AS (IF(deleted = 0, 1, NULL)) STORED
                COMMENT '未删除唯一约束标记';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'worker'
          AND INDEX_NAME = 'uk_phone'
    ) THEN
        ALTER TABLE worker DROP INDEX uk_phone;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'worker'
          AND INDEX_NAME = 'uk_phone_active'
    ) THEN
        ALTER TABLE worker ADD UNIQUE KEY uk_phone_active (phone, active_flag);
    END IF;
END//
DELIMITER ;

CALL fix_worker_phone_active_unique();
DROP PROCEDURE IF EXISTS fix_worker_phone_active_unique;
