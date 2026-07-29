package com.platform.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RolePermissionUpdateDTO {

    @NotNull(message = "权限ID列表不能为空")
    private List<Long> permissionIds = new ArrayList<>();
}
