package com.platform.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CustomerUpdateDTO {

    @JsonProperty("customerName")
    private String name;

    private String customerType;

    private String phone;

    private String idCard;

    @JsonProperty("enterpriseName")
    private String orgName;

    private String address;

    private Long dealerId;
}
