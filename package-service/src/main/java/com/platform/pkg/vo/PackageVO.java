package com.platform.pkg.vo;

import com.platform.pkg.enums.ChargeMode;
import com.platform.pkg.enums.PackageType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐 VO
 */
@Data
public class PackageVO {

    private Long id;

    private String packageCode;

    private String packageName;

    private String packageType;

    private String packageTypeDesc;

    private Long price;

    private Long faceValue;

    private Long flowQuota;

    private Integer durationDays;

    private String chargeMode;

    private String chargeModeDesc;

    private Long modelId;

    private String photoUrl;

    private Long commissionAmount;

    private BigDecimal commissionRate;

    private Long coldWaterRate;

    private Long hotWaterRate;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;

    /**
     * 从实体构建 VO
     */
    public static PackageVO fromEntity(com.platform.pkg.entity.WaterPackage entity) {
        PackageVO vo = new PackageVO();
        vo.setId(entity.getId());
        vo.setPackageCode(entity.getPackageCode());
        vo.setPackageName(entity.getPackageName());
        vo.setPackageType(entity.getPackageType());
        vo.setPackageTypeDesc(PackageType.getDescriptionByName(entity.getPackageType()));
        vo.setPrice(entity.getPrice());
        vo.setFaceValue(entity.getFaceValue());
        vo.setFlowQuota(entity.getFlowQuota());
        vo.setDurationDays(entity.getDurationDays());
        vo.setChargeMode(entity.getChargeMode());
        vo.setChargeModeDesc(ChargeMode.getDescriptionByName(entity.getChargeMode()));
        vo.setModelId(entity.getModelId());
        vo.setPhotoUrl(entity.getPhotoUrl());
        vo.setCommissionAmount(entity.getCommissionAmount());
        vo.setCommissionRate(entity.getCommissionRate());
        vo.setColdWaterRate(entity.getColdWaterRate());
        vo.setHotWaterRate(entity.getHotWaterRate());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setUpdatedBy(entity.getUpdatedBy());
        return vo;
    }
}
