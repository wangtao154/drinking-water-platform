package com.platform.pkg.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新套餐 DTO（全部字段可选）
 */
@Data
public class PackageUpdateDTO {

    /**
     * 套餐名称
     */
    private String packageName;

    /**
     * 套餐类型
     */
    private String packageType;

    /**
     * 价格（分）
     */
    @Min(value = 0, message = "价格不能为负数")
    private Long price;

    /**
     * 面值（分）
     */
    private Long faceValue;

    /**
     * 流量配额（升×1000）
     */
    @Min(value = 0, message = "流量配额不能为负数")
    private Long flowQuota;

    /**
     * 时长（天）
     */
    @Min(value = 1, message = "时长至少为1天")
    private Integer durationDays;

    /**
     * 计费模式
     */
    private String chargeMode;

    /**
     * 适用设备型号ID
     */
    private Long modelId;

    /**
     * 图片URL
     */
    private String photoUrl;

    /**
     * 固定分佣金额（分）
     */
    @Min(value = 0, message = "分佣金额不能为负数")
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
}
