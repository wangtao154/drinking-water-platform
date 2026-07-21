package com.platform.monitor.vo;

import com.platform.monitor.entity.DeviceAlert;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警 VO
 */
@Data
public class AlertVO implements Serializable {

    private Long id;

    private String deviceId;

    private String sn;

    private String alertType;

    private String alertLevel;

    private String alertMessage;

    private String pushStatus;

    private String handledStatus;

    private Long autoWorkOrderId;

    private String relatedFilterId;

    private LocalDateTime triggeredAt;

    private LocalDateTime pushedAt;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt;

    /**
     * 从实体构建 VO
     */
    public static AlertVO fromEntity(DeviceAlert entity) {
        AlertVO vo = new AlertVO();
        vo.setId(entity.getId());
        vo.setDeviceId(entity.getDeviceId());
        vo.setSn(entity.getSn());
        vo.setAlertType(entity.getAlertType());
        vo.setAlertLevel(entity.getAlertLevel());
        vo.setAlertMessage(entity.getAlertMessage());
        vo.setPushStatus(entity.getPushStatus());
        vo.setHandledStatus(entity.getHandledStatus());
        vo.setAutoWorkOrderId(entity.getAutoWorkOrderId());
        vo.setRelatedFilterId(entity.getRelatedFilterId());
        vo.setTriggeredAt(entity.getTriggeredAt());
        vo.setPushedAt(entity.getPushedAt());
        vo.setHandledAt(entity.getHandledAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
