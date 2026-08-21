-- 管理后台受控只读 AI 助手权限。
-- 可重复执行；默认授予超级管理员和平台管理员，其他角色在权限管理中按需分配。

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, path, sort_order, status)
VALUES (
    'AI_ASSISTANT_VIEW',
    'AI 助手使用',
    'API',
    0,
    '/api/v1/ai/assistant/**',
    99,
    'ENABLED'
)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    path = VALUES(path),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'AI_ASSISTANT_VIEW'
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN')
  AND r.deleted = 0
  AND p.deleted = 0
  AND p.status = 'ENABLED';
