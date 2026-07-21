package com.platform.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存批次实体
 */
@Data
@TableName("stock_batch")
public class StockBatch implements Serializable {

    /**
     * 主键ID（雪花算法自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 批次号（唯一）
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
}
