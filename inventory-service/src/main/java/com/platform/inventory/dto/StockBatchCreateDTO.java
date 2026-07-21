package com.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 库存批次创建 DTO
 */
@Data
public class StockBatchCreateDTO implements Serializable {

    /**
     * 批次类型：DEVICE/FILTER
     */
    @NotBlank(message = "批次类型不能为空")
    private String batchType;

    /**
     * 产品类型
     */
    @NotBlank(message = "产品类型不能为空")
    private String productType;

    /**
     * 数量
     */
    @NotNull(message = "数量不能为空")
    private Integer quantity;

    /**
     * 导入方式：MANUAL/EXCEL_IMPORT/CSV_IMPORT
     */
    private String importMethod;

    /**
     * 导入文件URL
     */
    private String importFileUrl;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 生产日期
     */
    private LocalDate producedAt;

    /**
     * 备注
     */
    private String remark;
}
