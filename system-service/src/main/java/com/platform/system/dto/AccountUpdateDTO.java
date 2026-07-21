package com.platform.system.dto;

import lombok.Data;

/**
 * 账户更新DTO（密码可选）
 */
@Data
public class AccountUpdateDTO {

    private String employeeNo;

    private String username;

    private String password;

    private Long roleId;

    private String name;

    private String phone;

    private String email;

    private String department;
}
