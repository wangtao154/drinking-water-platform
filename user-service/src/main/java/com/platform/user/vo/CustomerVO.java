package com.platform.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String customerName;

    private String customerType;

    private String phone;

    private String idCard;

    private String enterpriseName;

    private String address;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long dealerId;

    private String dealerName;

    private String status;

    private LocalDateTime registeredAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
