package com.platform.push.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推送任务 VO
 */
@Data
public class PushTaskVO implements Serializable {

    private Long alertId;

    private Long deviceId;

    private String alertType;

    private String alertLevel;

    private String alertMessage;

    private LocalDateTime triggeredAt;

    private String pushStatus;
}
