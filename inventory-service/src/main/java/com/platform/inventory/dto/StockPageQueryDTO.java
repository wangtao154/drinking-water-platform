package com.platform.inventory.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存分页查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockPageQueryDTO extends PageQueryDTO {

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 仓库类型：FACTORY/DEALER
     */
    private String warehouseType;

    /**
     * 库存状态：IN_STOCK/OUT_STOCK/TRANSFERRING
     */
    private String stockStatus;

    /**
     * 批次号
     */
    private String batchNo;
}
