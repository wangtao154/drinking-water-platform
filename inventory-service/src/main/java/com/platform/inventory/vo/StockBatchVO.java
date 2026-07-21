package com.platform.inventory.vo;

import com.platform.inventory.entity.StockBatch;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存批次 VO
 */
@Data
public class StockBatchVO implements Serializable {

    private Long id;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 批次类型：DEVICE/FILTER
     */
    private String batchType;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 数量
     */
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

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从实体构建 VO
     */
    public static StockBatchVO fromEntity(StockBatch entity) {
        StockBatchVO vo = new StockBatchVO();
        vo.setId(entity.getId());
        vo.setBatchNo(entity.getBatchNo());
        vo.setBatchType(entity.getBatchType());
        vo.setProductType(entity.getProductType());
        vo.setQuantity(entity.getQuantity());
        vo.setImportMethod(entity.getImportMethod());
        vo.setImportFileUrl(entity.getImportFileUrl());
        vo.setManufacturer(entity.getManufacturer());
        vo.setProducedAt(entity.getProducedAt());
        vo.setRemark(entity.getRemark());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
