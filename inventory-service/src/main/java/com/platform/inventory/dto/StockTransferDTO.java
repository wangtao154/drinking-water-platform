package com.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存调拨 DTO
 */
@Data
public class StockTransferDTO implements Serializable {

    /**
     * 设备ID
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 来源仓库类型：FACTORY/DEALER
     */
    @NotBlank(message = "来源仓库类型不能为空")
    private String fromWarehouseType;

    /**
     * 来源仓库ID
     */
    @NotNull(message = "来源仓库ID不能为空")
    private Long fromWarehouseId;

    /**
     * 目标仓库类型：FACTORY/DEALER
     */
    @NotBlank(message = "目标仓库类型不能为空")
    private String toWarehouseType;

    /**
     * 目标仓库ID
     */
    @NotNull(message = "目标仓库ID不能为空")
    private Long toWarehouseId;
}
