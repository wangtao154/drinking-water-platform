package com.platform.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户身份申请表
 * 游客通过微信登录后，可以申请成为正式客户或运维人员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_application")
public class Application extends BaseEntity {

    /** 申请者 customer.id */
    private Long applicantId;

    /** 申请类型：CUSTOMER=正式客户, WORKER=运维人员 */
    private String applyType;

    /** 姓名 */
    private String name;

    /** 手机号 */
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

    /** 状态：PENDING/APPROVED/REJECTED */
    private String status;

    /** 审批人ID */
    private Long reviewerId;

    /** 审批意见 */
    private String reviewComment;

    /** 审批时间 */
    private LocalDateTime reviewedAt;
}
