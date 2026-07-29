-- 修复 worker_wechat_binding 与逻辑删除不兼容的唯一约束。
-- 目标：仅 deleted=0 的有效绑定保持唯一，已删除记录不再占用 worker_id / official_open_id。

DROP PROCEDURE IF EXISTS fix_worker_wechat_binding_active_unique;
DELIMITER //
CREATE PROCEDURE fix_worker_wechat_binding_active_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'worker_wechat_binding'
          AND COLUMN_NAME = 'active_flag'
    ) THEN
        ALTER TABLE worker_wechat_binding
            ADD COLUMN active_flag TINYINT(1)
                GENERATED ALWAYS AS (IF(deleted = 0, 1, NULL)) STORED
                COMMENT '未删除唯一约束标记'
                AFTER deleted;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'worker_wechat_binding'
          AND INDEX_NAME = 'uk_worker_id'
    ) THEN
        ALTER TABLE worker_wechat_binding DROP INDEX uk_worker_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'worker_wechat_binding'
          AND INDEX_NAME = 'uk_official_open_id'
    ) THEN
        ALTER TABLE worker_wechat_binding DROP INDEX uk_official_open_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'worker_wechat_binding'
          AND INDEX_NAME = 'uk_worker_id_active'
    ) THEN
        ALTER TABLE worker_wechat_binding ADD UNIQUE KEY uk_worker_id_active (worker_id, active_flag);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'worker_wechat_binding'
          AND INDEX_NAME = 'uk_official_open_id_active'
    ) THEN
        ALTER TABLE worker_wechat_binding ADD UNIQUE KEY uk_official_open_id_active (official_open_id, active_flag);
    END IF;
END//
DELIMITER ;

CALL fix_worker_wechat_binding_active_unique();
DROP PROCEDURE IF EXISTS fix_worker_wechat_binding_active_unique;
