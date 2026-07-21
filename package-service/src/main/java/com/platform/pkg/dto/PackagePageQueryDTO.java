package com.platform.pkg.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 套餐分页查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PackagePageQueryDTO extends PageQueryDTO {

    /**
     * 套餐类型（可选）
     */
    private String packageType;

    /**
     * 状态（可选）：ENABLED / DISABLED
     */
    private String status;
}
