-- AI-04: 后台 AI 使用协议与隐私说明的版本化同意记录。
-- 只记录同意证据，不保存完整会话内容；账户删除后最少保留六个月。
CREATE TABLE IF NOT EXISTS ai_assistant_consent_log (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    account_id            BIGINT       NOT NULL                COMMENT '后台账户ID',
    account_name          VARCHAR(64)                          COMMENT '同意时账户名称快照',
    consent_type          VARCHAR(32)  NOT NULL                COMMENT 'AI_SERVICE_PRIVACY',
    policy_version        VARCHAR(32)  NOT NULL                COMMENT '协议与隐私说明版本',
    accepted              TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否同意：1同意',
    consent_source        VARCHAR(32)  NOT NULL                COMMENT 'LOGIN/ASSISTANT_PANEL',
    accepted_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次同意时间',
    last_confirmed_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近确认时间',
    account_cancelled_at  DATETIME(3)                          COMMENT '账户注销/删除时间',
    retention_until       DATETIME(3)  NOT NULL                COMMENT '最早清理时间',
    created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_consent_account_policy (account_id, consent_type, policy_version),
    KEY idx_ai_consent_retention_until (retention_until),
    KEY idx_ai_consent_accepted_at (accepted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台AI使用协议与隐私说明同意记录';
