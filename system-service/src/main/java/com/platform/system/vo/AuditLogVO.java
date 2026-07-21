package com.platform.system.vo;

import com.platform.system.entity.AuditLog;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志 VO
 */
@Data
public class AuditLogVO implements Serializable {

    private Long id;

    private String traceId;

    private Long operatorId;

    private String operatorType;

    private String operatorName;

    private String operation;

    private String targetType;

    private String targetId;

    private String requestIp;

    private String requestUrl;

    private String requestMethod;

    private String requestParams;

    private Integer responseCode;

    private Integer costTime;

    private String result;

    private String errorMsg;

    private LocalDateTime createdAt;

    /**
     * 从实体构建 VO
     */
    public static AuditLogVO fromEntity(AuditLog entity) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(entity.getId());
        vo.setTraceId(entity.getTraceId());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorType(entity.getOperatorType());
        vo.setOperatorName(entity.getOperatorName());
        vo.setOperation(entity.getOperation());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setRequestIp(entity.getRequestIp());
        vo.setRequestUrl(entity.getRequestUrl());
        vo.setRequestMethod(entity.getRequestMethod());
        vo.setRequestParams(entity.getRequestParams());
        vo.setResponseCode(entity.getResponseCode());
        vo.setCostTime(entity.getCostTime());
        vo.setResult(entity.getResult());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
