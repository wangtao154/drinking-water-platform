package com.platform.user.dto;

import lombok.Data;

@Data
public class SysUserUpdateDTO {

    private String name;
    private String phone;
    private String email;
    private Long roleId;
    private String department;
    private String status;
}
