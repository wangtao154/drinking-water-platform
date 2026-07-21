package com.platform.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerCreateDTO {

    @JsonProperty("customerName")
    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String address;

    @NotBlank(message = "客户类型不能为空")
    private String customerType;

    private String idCard;

    @JsonProperty("enterpriseName")
    private String orgName;

    private Long dealerId;

    private String deviceId;
}
