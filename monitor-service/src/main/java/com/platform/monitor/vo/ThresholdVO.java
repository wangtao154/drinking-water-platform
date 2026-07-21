package com.platform.monitor.vo;

import com.platform.monitor.entity.AlertThreshold;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警阈值 VO
 */
@Data
public class ThresholdVO implements Serializable {

    private Long id;

    private String thresholdKey;

    private String thresholdValue;

    private String relatedPoint;

    private String description;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 从实体构建 VO
     */
    public static ThresholdVO fromEntity(AlertThreshold entity) {
        ThresholdVO vo = new ThresholdVO();
        vo.setId(entity.getId());
        vo.setThresholdKey(entity.getThresholdKey());
        vo.setThresholdValue(entity.getThresholdValue());
        vo.setRelatedPoint(entity.getRelatedPoint());
        vo.setDescription(entity.getDescription());
        vo.setEnabled(entity.getEnabled());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
