-- 为所有缺少 created_by/updated_by 的业务表添加这两列
-- 使用存储过程确保幂等（只添加尚不存在的列）

DELIMITER //
DROP PROCEDURE IF EXISTS add_audit_columns//
CREATE PROCEDURE add_audit_columns()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE tbl_name VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT t.TABLE_NAME
        FROM information_schema.TABLES t
        WHERE t.TABLE_SCHEMA = 'drinking_water'
        AND t.TABLE_TYPE = 'BASE TABLE'
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS c
            WHERE c.TABLE_SCHEMA = t.TABLE_SCHEMA
            AND c.TABLE_NAME = t.TABLE_NAME
            AND c.COLUMN_NAME = 'created_by'
        )
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS c
            WHERE c.TABLE_SCHEMA = t.TABLE_SCHEMA
            AND c.TABLE_NAME = t.TABLE_NAME
            AND c.COLUMN_NAME = 'created_at'
        );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN `created_by` BIGINT NULL COMMENT ''创建人'', ADD COLUMN `updated_by` BIGINT NULL COMMENT ''更新人''');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END//
DELIMITER ;

CALL add_audit_columns();
DROP PROCEDURE IF EXISTS add_audit_columns;
