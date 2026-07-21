package com.platform.pkg.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 套餐实体
 * 注意：表名 package 是 SQL 关键字，必须用反引号
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`package`")
public class WaterPackage extends BaseEntity {

    /**
     * 套餐编号（PK+日期序号）
     */
    private String packageCode;

    /**
     * 套餐名称
     */
    private String packageName;

    /**
     * 套餐类型：QR_SCAN / SHARED / RENTAL / WALLET / INSTALL
     */
    private String packageType;

    /**
     * 价格（分）
     */
    private Long price;

    /**
     * 面值（分）
     */
    private Long faceValue;

    /**
     * 流量配额（升×1000）
     */
    private Long flowQuota;

    /**
     * 时长（天）
     */
    private Integer durationDays;

    /**
     * 计费模式：FLOW_BASED / MONTHLY_RENT / PACKAGE_RECHARGE / SHARED
     */
    private String chargeMode;

    /**
     * 适用设备型号ID（null=通用）
     */
    private Long modelId;

    /**
     * 图片URL
     */
    private String photoUrl;

    /**
     * 固定分佣金额（分）
     */
    private Long commissionAmount;

    /**
     * 分佣比例
     */
    private BigDecimal commissionRate;

    /**
     * 冷水费率（分/升×1000）
     */
    private Long coldWaterRate;

    /**
     * 热水费率（分/升×1000）
     */
    private Long hotWaterRate;

    /**
     * 状态：ENABLED / DISABLED
     */
    private String status;
}
