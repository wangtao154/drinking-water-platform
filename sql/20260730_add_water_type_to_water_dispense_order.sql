ALTER TABLE water_dispense_order
    ADD COLUMN water_type TINYINT NOT NULL DEFAULT 0 COMMENT 'Water type: 0=cold, 1=hot'
    AFTER target_ml;
