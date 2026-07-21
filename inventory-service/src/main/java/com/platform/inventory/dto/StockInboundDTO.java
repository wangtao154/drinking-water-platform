package com.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存入库 DTO
 */
@Data
public class StockInboundDTO implements Serializable {

    /**
     * 设备ID
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 批次号
     */
    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    /**
     * 仓库类型：FACTORY/DEALER
     */
    @NotBlank(message = "仓库类型不能为空")
    private String warehouseType;

    /**
     * 仓库ID
     */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /**
     * 入库数量
     */
    @NotNull(message = "数量不能为空")
    private Integer quantity;
}
