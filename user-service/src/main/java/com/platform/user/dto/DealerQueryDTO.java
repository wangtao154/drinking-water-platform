package com.platform.user.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DealerQueryDTO extends PageQueryDTO {

    private String dealerName;
    private String dealerLevel;
    private Long parentId;
}
