package com.platform.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DealerVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String dealerCode;
    private String dealerName;
    private String dealerLevel;
    private Integer level;
    private Long parentId;
    private String phone;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private BigDecimal commissionRate;
    private String region;
    private String status;
    private LocalDateTime registeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
