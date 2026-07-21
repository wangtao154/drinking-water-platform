package com.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkerCreateDTO {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotNull(message = "所属经销商不能为空")
    private Long dealerId;

    private String workType;

    private String idCard;

    @NotBlank(message = "密码不能为空")
    private String password;
}
