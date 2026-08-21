-- RBAC 权限字典补齐脚本
-- 适用于已有 drinking_water 数据库，重复执行安全。

USE drinking_water;
SET NAMES utf8mb4;

-- 兼容早期 init.sql：RBAC 两张表最初没有完整的 BaseEntity 字段。
SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_permission ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0未删，1已删''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'deleted'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_permission ADD COLUMN created_by BIGINT NULL COMMENT ''创建人ID''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'created_by'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_permission ADD COLUMN updated_by BIGINT NULL COMMENT ''更新人ID''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'updated_by'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_role_permission ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT ''更新时间''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_role_permission' AND COLUMN_NAME = 'updated_at'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_role_permission ADD COLUMN created_by BIGINT NULL COMMENT ''创建人ID''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_role_permission' AND COLUMN_NAME = 'created_by'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_role_permission ADD COLUMN updated_by BIGINT NULL COMMENT ''更新人ID''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_role_permission' AND COLUMN_NAME = 'updated_by'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_role_permission ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0未删，1已删''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_role_permission' AND COLUMN_NAME = 'deleted'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 顶级菜单
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, path, icon, sort_order, status) VALUES
('DASHBOARD', '仪表盘', 'MENU', 0, '/dashboard', 'Odometer', 1, 'ENABLED'),
('DEVICE', '设备管理', 'MENU', 0, '/devices', 'Box', 2, 'ENABLED'),
('FILTER', '滤芯管理', 'MENU', 0, '/filters', 'Filter', 3, 'ENABLED'),
('CUSTOMER', '客户管理', 'MENU', 0, '/customers', 'User', 4, 'ENABLED'),
('APPLICATION', '身份审批', 'MENU', 0, '/applications', 'Stamp', 5, 'ENABLED'),
('DEALER', '经销商管理', 'MENU', 0, '/dealers', 'OfficeBuilding', 6, 'ENABLED'),
('WORKER', '运维人员', 'MENU', 0, '/workers', 'UserFilled', 7, 'ENABLED'),
('WORK_ORDER', '工单管理', 'MENU', 0, '/work-orders', 'Document', 8, 'ENABLED'),
('ORDER', '订单管理', 'MENU', 0, '/orders', 'ShoppingCart', 9, 'ENABLED'),
('PACKAGE', '套餐管理', 'MENU', 0, '/packages', 'Goods', 10, 'ENABLED'),
('INVENTORY', '库存管理', 'MENU', 0, '/inventory', 'Box', 11, 'ENABLED'),
('FINANCE', '财务管理', 'MENU', 0, '/finance', 'Money', 12, 'ENABLED'),
('REPORT', '报表中心', 'MENU', 0, '/reports', 'DataAnalysis', 13, 'ENABLED'),
('MONITOR', '监控预警', 'MENU', 0, '/monitor', 'Monitor', 14, 'ENABLED'),
('SYSTEM', '系统管理', 'MENU', 0, '/system', 'Setting', 15, 'ENABLED')
ON DUPLICATE KEY UPDATE
permission_name = VALUES(permission_name),
permission_type = VALUES(permission_type),
parent_id = VALUES(parent_id),
path = VALUES(path),
icon = VALUES(icon),
sort_order = VALUES(sort_order),
status = VALUES(status);

-- 子菜单、按钮、API 权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, path, sort_order, status) VALUES
('DEVICE_VIEW', '设备查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'DEVICE'), '/api/v1/devices', 1, 'ENABLED'),
('DEVICE_EDIT', '设备维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'DEVICE'), '/api/v1/devices/*', 2, 'ENABLED'),
('DEVICE_DELETE', '设备删除', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'DEVICE'), NULL, 3, 'ENABLED'),
('DEVICE_CONFIG_WRITE', '设备配置下发', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'DEVICE'), NULL, 4, 'ENABLED'),

('FILTER_VIEW', '滤芯查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'FILTER'), '/api/v1/filters', 1, 'ENABLED'),
('FILTER_EDIT', '滤芯维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'FILTER'), '/api/v1/filters/*', 2, 'ENABLED'),
('FILTER_DELETE', '滤芯删除', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'FILTER'), NULL, 3, 'ENABLED'),

('CUSTOMER_VIEW', '客户查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'CUSTOMER'), '/api/v1/customers', 1, 'ENABLED'),
('CUSTOMER_EDIT', '客户维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'CUSTOMER'), '/api/v1/customers/*', 2, 'ENABLED'),
('CUSTOMER_DELETE', '客户删除', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'CUSTOMER'), NULL, 3, 'ENABLED'),

('APPLICATION_VIEW', '申请查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'APPLICATION'), '/api/v1/applications', 1, 'ENABLED'),
('APPLICATION_APPROVE', '申请审批', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'APPLICATION'), NULL, 2, 'ENABLED'),
('APPLICATION_DELETE', '申请删除', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'APPLICATION'), NULL, 3, 'ENABLED'),

('DEALER_VIEW', '经销商查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'DEALER'), '/api/v1/dealers', 1, 'ENABLED'),
('DEALER_EDIT', '经销商维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'DEALER'), '/api/v1/dealers/*', 2, 'ENABLED'),
('DEALER_DELETE', '经销商删除', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'DEALER'), NULL, 3, 'ENABLED'),

('WORKER_VIEW', '运维查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'WORKER'), '/api/v1/workers', 1, 'ENABLED'),
('WORKER_EDIT', '运维维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'WORKER'), '/api/v1/workers/*', 2, 'ENABLED'),
('WORKER_DELETE', '运维删除', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'WORKER'), NULL, 3, 'ENABLED'),

('WORK_ORDER_VIEW', '工单查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'WORK_ORDER'), '/api/v1/work-orders', 1, 'ENABLED'),
('WORK_ORDER_ASSIGN', '工单派单', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'WORK_ORDER'), NULL, 2, 'ENABLED'),
('WORK_ORDER_EDIT', '工单维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'WORK_ORDER'), '/api/v1/work-orders/*', 3, 'ENABLED'),
('WORK_ORDER_DELETE', '工单删除', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'WORK_ORDER'), NULL, 4, 'ENABLED'),

('ORDER_VIEW', '订单查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'ORDER'), '/api/v1/orders', 1, 'ENABLED'),
('ORDER_SCAN', '扫码订单', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'ORDER'), '/api/v1/water/dispense/orders', 2, 'ENABLED'),
('ORDER_REFUND', '订单退款', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'ORDER'), NULL, 3, 'ENABLED'),

('PACKAGE_VIEW', '套餐查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'PACKAGE'), '/api/v1/packages', 1, 'ENABLED'),
('PACKAGE_EDIT', '套餐维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'PACKAGE'), '/api/v1/packages/*', 2, 'ENABLED'),
('PACKAGE_DELETE', '套餐删除', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'PACKAGE'), NULL, 3, 'ENABLED'),

('INVENTORY_VIEW', '库存查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'INVENTORY'), '/api/v1/inventory', 1, 'ENABLED'),
('INVENTORY_EDIT', '库存维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'INVENTORY'), '/api/v1/inventory/*', 2, 'ENABLED'),

('FINANCE_VIEW', '财务查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'FINANCE'), '/api/v1/finance', 1, 'ENABLED'),
('FINANCE_EDIT', '财务维护', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'FINANCE'), '/api/v1/finance/*', 2, 'ENABLED'),

('REPORT_VIEW', '报表查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'REPORT'), '/api/v1/reports', 1, 'ENABLED'),

('MONITOR_VIEW', '监控查看', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'MONITOR'), '/api/v1/monitor', 1, 'ENABLED'),
('MONITOR_EDIT', '监控配置', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'MONITOR'), '/api/v1/monitor/*', 2, 'ENABLED'),

('SYSTEM_USER', '账户管理', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'SYSTEM'), '/api/v1/system/users', 1, 'ENABLED'),
('SYSTEM_ROLE', '角色管理', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'SYSTEM'), '/api/v1/system/roles', 2, 'ENABLED'),
('SYSTEM_ROLE_PERMISSION', '角色授权', 'BUTTON', (SELECT id FROM sys_permission p WHERE p.permission_code = 'SYSTEM'), NULL, 3, 'ENABLED'),
('SYSTEM_CONFIG', '系统配置', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'SYSTEM'), '/api/v1/system/configs', 4, 'ENABLED'),
('SYSTEM_AUDIT', '审计日志', 'API', (SELECT id FROM sys_permission p WHERE p.permission_code = 'SYSTEM'), '/api/v1/system/audit-logs', 5, 'ENABLED'),
('AI_ASSISTANT_VIEW', 'AI 助手使用', 'API', 0, '/api/v1/ai/assistant/**', 99, 'ENABLED')
ON DUPLICATE KEY UPDATE
permission_name = VALUES(permission_name),
permission_type = VALUES(permission_type),
parent_id = VALUES(parent_id),
path = VALUES(path),
sort_order = VALUES(sort_order),
status = VALUES(status);

-- 超级管理员始终拥有全部权限。
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.status = 'ENABLED' AND p.deleted = 0
WHERE r.role_code = 'SUPER_ADMIN' AND r.deleted = 0;
