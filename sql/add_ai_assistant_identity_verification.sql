-- AI-02: 后台 AI 使用者真实身份核验与可追溯。
-- 执行后，只有已由管理员核验“真实姓名 + 绑定手机号”的后台账户可以调用 AI 助手。
DROP PROCEDURE IF EXISTS ensure_ai_assistant_identity_verification;
DELIMITER //
CREATE PROCEDURE ensure_ai_assistant_identity_verification()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'identity_verified'
    ) THEN
        ALTER TABLE sys_user ADD COLUMN identity_verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已完成姓名和手机号人工核验' AFTER status;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'identity_verified_at'
    ) THEN
        ALTER TABLE sys_user ADD COLUMN identity_verified_at DATETIME(3) NULL COMMENT '最近一次身份核验时间' AFTER identity_verified;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'identity_verified_by'
    ) THEN
        ALTER TABLE sys_user ADD COLUMN identity_verified_by BIGINT NULL COMMENT '最近一次核验操作人ID' AFTER identity_verified_at;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'identity_verification_method'
    ) THEN
        ALTER TABLE sys_user ADD COLUMN identity_verification_method VARCHAR(64) NULL COMMENT '核验方式' AFTER identity_verified_by;
    END IF;
END//
DELIMITER ;
CALL ensure_ai_assistant_identity_verification();
DROP PROCEDURE IF EXISTS ensure_ai_assistant_identity_verification;

CREATE TABLE IF NOT EXISTS sys_user_identity_verification_log (
    id                      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    account_id              BIGINT        NOT NULL                COMMENT '被核验后台账户ID',
    verified                TINYINT(1)    NOT NULL                COMMENT '核验结果：1已核验/0已撤销或失效',
    verification_method     VARCHAR(64)   NOT NULL                COMMENT '核验方式或失效原因',
    verified_by             BIGINT                                COMMENT '核验操作人ID',
    verified_at             DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
    account_name            VARCHAR(64)                           COMMENT '核验时账户姓名快照',
    phone_masked            VARCHAR(32)                           COMMENT '核验时手机号脱敏快照',
    PRIMARY KEY (id),
    KEY idx_identity_log_account_id (account_id),
    KEY idx_identity_log_verified_at (verified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台AI使用者身份核验记录';
