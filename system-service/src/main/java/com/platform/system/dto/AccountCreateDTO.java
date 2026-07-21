package com.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountCreateDTO {

    @NotBlank(message = "工号不能为空")
    private String employeeNo;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Long roleId;

    private String name;

    private String phone;

    private String email;

    private String department;
}
