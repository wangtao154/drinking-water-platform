package com.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户身份申请 DTO（小程序端提交）
 */
@Data
public class ApplicationCreateDTO {

    /** 申请类型：CUSTOMER=正式客户, WORKER=运维人员 */
    @NotBlank(message = "申请类型不能为空")
    private String applyType;

    /** 姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 身份证号 */
    private String idCard;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 详细地址 */
    private String address;

    /** 客户类型(仅客户申请)：PERSONAL/FAMILY/COMPANY/SCHOOL/OTHER_ORG */
    private String customerType;

    /** 单位名称(仅单位客户) */
    private String orgName;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 所属经销商(仅运维申请) */
    private Long dealerId;
}
