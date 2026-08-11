ALTER TABLE water_dispense_order
    ADD COLUMN refund_status VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'Refund status: NONE/PROCESSING/SUCCESS/FAILED' AFTER mock_payment,
    ADD COLUMN refund_no VARCHAR(64) NULL COMMENT 'WeChat refund request number' AFTER refund_status,
    ADD COLUMN wechat_refund_id VARCHAR(64) NULL COMMENT 'WeChat refund id' AFTER refund_no,
    ADD COLUMN refund_amount BIGINT NULL COMMENT 'Refund amount in cents' AFTER wechat_refund_id,
    ADD COLUMN refund_reason VARCHAR(255) NULL COMMENT 'Refund reason' AFTER refund_amount,
    ADD COLUMN refund_requested_at DATETIME(3) NULL COMMENT 'Refund requested time' AFTER refund_reason,
    ADD COLUMN refund_success_at DATETIME(3) NULL COMMENT 'Refund success time' AFTER refund_requested_at,
    ADD COLUMN refund_error_msg VARCHAR(512) NULL COMMENT 'Refund error message' AFTER refund_success_at,
    ADD UNIQUE KEY uk_water_dispense_refund_no (refund_no),
    ADD KEY idx_water_dispense_refund_status (refund_status);
