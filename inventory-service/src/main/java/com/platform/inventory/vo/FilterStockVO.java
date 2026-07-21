package com.platform.inventory.vo;

import com.platform.inventory.entity.FilterStock;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 滤芯库存 VO
 */
@Data
public class FilterStockVO implements Serializable {

    private Long id;

    /**
     * 滤芯ID
     */
    private String filterId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 仓库类型：FACTORY/DEALER
     */
    private String warehouseType;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 库存状态：IN_STOCK/OUT_STOCK/TRANSFERRING
     */
    private String stockStatus;

    /**
     * 入库时间
     */
    private LocalDateTime inboundAt;

    /**
     * 出库时间
     */
    private LocalDateTime outboundAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体构建 VO
     */
    public static FilterStockVO fromEntity(FilterStock entity) {
        FilterStockVO vo = new FilterStockVO();
        vo.setId(entity.getId());
        vo.setFilterId(entity.getFilterId());
        vo.setBatchNo(entity.getBatchNo());
        vo.setWarehouseType(entity.getWarehouseType());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setStockStatus(entity.getStockStatus());
        vo.setInboundAt(entity.getInboundAt());
        vo.setOutboundAt(entity.getOutboundAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
