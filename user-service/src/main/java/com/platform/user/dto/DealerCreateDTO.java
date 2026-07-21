package com.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DealerCreateDTO {

    @NotBlank(message = "经销商编号不能为空")
    private String dealerCode;

    @NotBlank(message = "经销商名称不能为空")
    private String dealerName;

    private Long parentId;

    /** 联系人姓名（前端字段 contactPerson） */
    private String contactPerson;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    /** 地址（前端字段 address，映射到 dealer.region） */
    private String address;

    /** 经销商等级（前端数字，如 1/2/3，映射到 L1_DEALER/L2_DEALER/L3_DEALER） */
    @NotNull(message = "经销商等级不能为空")
    private Integer level;

    private BigDecimal commissionRate;

    /** 登录手机号（前端未提供时，默认使用 contactPhone） */
    private String phone;

    /** 登录密码（前端未提供时，默认使用 contactPhone 后六位） */
    private String password;
}
