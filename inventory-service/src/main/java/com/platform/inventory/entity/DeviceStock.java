package com.platform.inventory.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备库存实体
 * 注意：该表无 deleted/created_by/updated_by 字段，不继承 BaseEntity
 */
@Data
@TableName("device_stock")
public class DeviceStock implements Serializable {

    /**
     * 主键ID（雪花算法自动生成）
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
