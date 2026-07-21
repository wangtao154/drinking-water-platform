package com.platform.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备库存流转日志
 */
@Data
@TableName("device_stock_log")
public class DeviceStockLog implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 来源仓库
     */
    private String fromWarehouse;

    /**
     * 目标仓库
     */
    private String toWarehouse;

    /**
     * 操作类型：INBOUND/OUTBOUND/TRANSFER/DISTRIBUTE
     */
    private String operation;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
