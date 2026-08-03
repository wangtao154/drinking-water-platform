package com.platform.iot.vo;

import lombok.Data;

@Data
public class CommandAckResultVO {

    private Boolean acknowledged;

    private Boolean ackReceived;

    private Integer attempts;

    private Integer maxAttempts;

    private Long ackTimeoutMs;

    private String lastMessageId;

    private String status;

    private String errorMessage;

    public static CommandAckResultVO pending(int maxAttempts, long ackTimeoutMs) {
        CommandAckResultVO vo = new CommandAckResultVO();
        vo.setAcknowledged(false);
        vo.setAckReceived(false);
        vo.setAttempts(0);
        vo.setMaxAttempts(maxAttempts);
        vo.setAckTimeoutMs(ackTimeoutMs);
        vo.setStatus("PENDING");
        return vo;
    }
}
