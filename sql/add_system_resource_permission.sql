SET NAMES utf8mb4;
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, path, sort_order, status)
SELECT 'SYSTEM_RESOURCE_VIEW', CONVERT(0xE7B3BBE7BB9FE8B584E6BA90 USING utf8mb4), 'API', id,
       '/api/v1/system/resources', 5, 'ENABLED'
FROM (SELECT id FROM sys_permission WHERE permission_code='SYSTEM' AND deleted=0) AS parent
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name), path=VALUES(path);
INSERT IGNORE INTO sys_role_permission (role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p ON p.permission_code='SYSTEM_RESOURCE_VIEW'
WHERE r.role_code IN ('SUPER_ADMIN','ADMIN') AND r.deleted=0 AND p.deleted=0;
