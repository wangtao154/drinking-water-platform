SET @schema_name = DATABASE();

SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'water_dispense_order'
      AND COLUMN_NAME = 'payment_provider'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN payment_provider VARCHAR(32) NULL COMMENT ''Payment provider: MOCK/WECHAT'' AFTER transaction_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'water_dispense_order'
      AND COLUMN_NAME = 'prepay_id'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN prepay_id VARCHAR(128) NULL COMMENT ''WeChat Pay prepay_id'' AFTER payment_provider',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'water_dispense_order'
      AND COLUMN_NAME = 'wx_open_id'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE water_dispense_order ADD COLUMN wx_open_id VARCHAR(128) NULL COMMENT ''Payer mini program openid'' AFTER prepay_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'water_dispense_order'
      AND INDEX_NAME = 'idx_water_dispense_prepay_id'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_water_dispense_prepay_id ON water_dispense_order (prepay_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'water_dispense_order'
      AND INDEX_NAME = 'idx_water_dispense_transaction_id'
);
SET @ddl = IF(
    @index_exists > 0,
    'ALTER TABLE water_dispense_order DROP INDEX idx_water_dispense_transaction_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'water_dispense_order'
      AND INDEX_NAME = 'uk_water_dispense_transaction_id'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE UNIQUE INDEX uk_water_dispense_transaction_id ON water_dispense_order (transaction_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
