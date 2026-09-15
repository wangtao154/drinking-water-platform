-- 外部受控 AI 接口：密钥只保存 SHA-256 哈希，完整密钥仅在创建时返回一次。
CREATE TABLE IF NOT EXISTS ai_assistant_api_key (
    id                          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    key_id                      VARCHAR(24)   NOT NULL COMMENT '公开密钥标识，不包含密钥内容',
    key_prefix                  VARCHAR(32)   NOT NULL COMMENT '前端展示前缀',
    key_hash                    CHAR(64)      NOT NULL COMMENT '密钥随机部分SHA-256哈希',
    name                        VARCHAR(64)   NOT NULL COMMENT '密钥名称',
    owner_id                    BIGINT        NOT NULL COMMENT '创建并绑定的后台账户ID',
    scope                       VARCHAR(64)   NOT NULL DEFAULT 'AI_CHAT_READONLY' COMMENT '仅允许只读AI对话',
    status                      VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED/REVOKED',
    rate_limit_per_minute       INT           NOT NULL DEFAULT 30 COMMENT '每分钟最大调用数',
    expires_at                  DATETIME(3)   NULL COMMENT '失效时间，空表示不过期',
    last_used_at                DATETIME(3)   NULL COMMENT '最近使用时间',
    last_used_ip                VARCHAR(64)   NULL COMMENT '最近使用IP脱敏值',
    created_by                  BIGINT        NOT NULL COMMENT '创建人后台账户ID',
    revoked_at                  DATETIME(3)   NULL COMMENT '吊销时间',
    created_at                  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at                  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_assistant_api_key_id (key_id),
    KEY idx_ai_assistant_api_key_owner (owner_id),
    KEY idx_ai_assistant_api_key_status_expiry (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部AI助手API Key，仅保存哈希';

CREATE TABLE IF NOT EXISTS ai_assistant_api_access_log (
    id                          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    api_key_id                  BIGINT        NULL COMMENT '调用的API Key ID；无效密钥尝试为空',
    key_hint                    VARCHAR(32)   NULL COMMENT '密钥脱敏前缀或missing/malformed，不保存密钥',
    request_id                  VARCHAR(64)   NULL COMMENT '关联AI审计请求ID',
    endpoint                    VARCHAR(128)  NOT NULL COMMENT '访问接口',
    result_status               VARCHAR(24)   NOT NULL COMMENT 'SUCCESS/FALLBACK/REJECTED/ERROR',
    response_code               INT           NULL COMMENT 'HTTP响应码',
    elapsed_ms                  BIGINT        NULL COMMENT '处理耗时毫秒',
    client_ip_masked            VARCHAR(64)   NULL COMMENT '来源IP脱敏值',
    account_cancelled_at        DATETIME(3)   NULL COMMENT '绑定账户注销时间',
    retention_until             DATETIME(3)   NOT NULL COMMENT '最早清理时间，不少于6个月',
    created_at                  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ai_assistant_api_access_key_created (api_key_id, created_at),
    KEY idx_ai_assistant_api_access_request (request_id),
    KEY idx_ai_assistant_api_access_retention (retention_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部AI助手API调用审计日志（不保存完整问题和回答）';

-- 仅超级管理员和平台管理员可管理外部密钥；其他角色可在权限管理中按需授权。
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, path, sort_order, status)
VALUES ('AI_ASSISTANT_API_KEY_MANAGE', CONVERT(0x4149E5BC80E694BEE68EA5E58FA3E5AF86E992A5E7AEA1E79086 USING utf8mb4), 'API', 0,
        '/api/v1/ai/assistant/api-keys/**', 100, 'ENABLED')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name), permission_type = VALUES(permission_type),
    path = VALUES(path), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'AI_ASSISTANT_API_KEY_MANAGE'
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN')
  AND r.deleted = 0
  AND p.deleted = 0
  AND p.status = 'ENABLED';
