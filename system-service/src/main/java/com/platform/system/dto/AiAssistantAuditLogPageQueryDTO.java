package com.platform.system.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI assistant compliance audit log page query. */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiAssistantAuditLogPageQueryDTO extends PageQueryDTO {

    private Long operatorId;
    private String modelName;
    private String resultStatus;
    private String startTime;
    private String endTime;
}
