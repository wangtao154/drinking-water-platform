package com.platform.system.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 助手投诉或举报分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiAssistantComplaintPageQueryDTO extends PageQueryDTO {

    private String category;
    private String status;
    private String keyword;
    private String startTime;
    private String endTime;
}
