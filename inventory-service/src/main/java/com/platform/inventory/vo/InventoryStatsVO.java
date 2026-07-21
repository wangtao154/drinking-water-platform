package com.platform.inventory.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 库存统计 VO
 */
@Data
public class InventoryStatsVO implements Serializable {

    /**
     * 设备库存总数
     */
    private Long totalDeviceStock;

    /**
     * 在库设备数
     */
    private Long inStockDevices;

    /**
     * 出库设备数
     */
    private Long outStockDevices;

    /**
     * 调拨中设备数
     */
    private Long transferringDevices;

    /**
     * 滤芯库存总数
     */
    private Long totalFilterStock;

    /**
     * 在库滤芯数
     */
    private Long inStockFilters;
}
