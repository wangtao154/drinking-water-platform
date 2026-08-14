package com.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountCreateDTO {

    @NotBlank(message = "工号不能为空")
    private String employeeNo;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotNull(message = "\u8bf7\u9009\u62e9\u89d2\u8272")
    private Long roleId;

    private String name;

    private String phone;

    private String email;

    private String department;
}
