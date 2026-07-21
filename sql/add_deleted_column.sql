-- 为所有缺少 deleted 列的表添加 deleted TINYINT NOT NULL DEFAULT 0

DELIMITER //
DROP PROCEDURE IF EXISTS add_deleted_column//
CREATE PROCEDURE add_deleted_column()
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
            AND c.COLUMN_NAME = 'deleted'
        );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END//
DELIMITER ;

CALL add_deleted_column();
DROP PROCEDURE IF EXISTS add_deleted_column;
