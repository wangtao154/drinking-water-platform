package com.platform.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {

    private String customerType;
    private String name;
    private String phone;

    /** 身份证号 */
    private String idCard;

    @JsonIgnore
    private String password;

    private Boolean passwordChanged;
    private String wechat;
    private String orgName;
    private String creditCode;
    private String contactName;
    private String contactPhone;
    private String contactWechat;
    private String bankAccount;
    private Long balance;
    private Integer remainDuration;
    private Long remainFlow;
    private String province;
    private String city;
    private String district;
    private String address;
    private String openId;
    private String unionId;
    private Long dealerId;
    private String status;
    private LocalDateTime registeredAt;

    /**
     * 兼容前端字段 customerName
     */
    @JsonProperty("customerName")
    public String getCustomerName() {
        return name;
    }
}
