package com.platform.pkg.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建套餐 DTO
 */
@Data
public class PackageCreateDTO {

    /**
     * 套餐名称
     */
    @NotBlank(message = "套餐名称不能为空")
    private String packageName;

    /**
     * 套餐类型：QR_SCAN / SHARED / RENTAL / WALLET / INSTALL
     */
    @NotBlank(message = "套餐类型不能为空")
    private String packageType;

    /**
     * 价格（分）
     */
    @NotNull(message = "价格不能为空")
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
