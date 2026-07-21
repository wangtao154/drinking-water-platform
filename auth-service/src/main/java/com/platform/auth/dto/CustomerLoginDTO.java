package com.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 客户手机号密码登录 DTO
 */
@Data
public class CustomerLoginDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}
