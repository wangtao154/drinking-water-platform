package com.platform.water.vo;

import lombok.Data;

@Data
public class IotCommandAckResultVO {

    private Boolean acknowledged;

    private Boolean ackReceived;

    private Integer attempts;

    private Integer maxAttempts;

    private Long ackTimeoutMs;

    private String lastMessageId;

    private String status;

    private String errorMessage;
}
