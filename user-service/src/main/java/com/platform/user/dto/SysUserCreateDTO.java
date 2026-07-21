package com.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysUserCreateDTO {

    @NotBlank(message = "登录账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String phone;
    private String email;

    @NotNull(message = "角色不能为空")
    private Long roleId;

    private String department;

    @NotBlank(message = "工号不能为空")
    private String employeeNo;
}
