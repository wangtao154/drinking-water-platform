package com.platform.user.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkerQueryDTO extends PageQueryDTO {

    private String name;
    private String phone;
    private Long dealerId;
    private String workType;
}
