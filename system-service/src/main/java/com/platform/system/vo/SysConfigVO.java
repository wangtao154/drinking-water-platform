package com.platform.system.vo;

import com.platform.system.entity.SysConfig;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置 VO
 */
@Data
public class SysConfigVO implements Serializable {

    private Long id;

    private String configKey;

    private String configValue;

    private String configDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 从实体构建 VO
     */
    public static SysConfigVO fromEntity(SysConfig entity) {
        SysConfigVO vo = new SysConfigVO();
        vo.setId(entity.getId());
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigValue(entity.getConfigValue());
        vo.setConfigDesc(entity.getConfigDesc());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
