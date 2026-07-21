package com.platform.user.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQueryDTO extends PageQueryDTO {

    private String username;
    private String name;
    private Long roleId;
    private String status;
}
