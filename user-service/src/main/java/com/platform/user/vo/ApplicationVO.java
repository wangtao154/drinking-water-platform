package com.platform.user.vo;

import com.platform.user.entity.Application;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申请列表 VO
 */
@Data
public class ApplicationVO {

    private Long id;
    private Long applicantId;
    private String applyType;
    private String name;
    private String phone;
    private String idCard;
    private String province;
    private String city;
    private String district;
    private String address;
    private String customerType;
    private String orgName;
    private String creditCode;
    private Long dealerId;
    private String status;
    private String reviewComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    /** 经销商名称（关联查询） */
    private String dealerName;

    public static ApplicationVO from(Application app) {
        ApplicationVO vo = new ApplicationVO();
        vo.setId(app.getId());
        vo.setApplicantId(app.getApplicantId());
        vo.setApplyType(app.getApplyType());
        vo.setName(app.getName());
        vo.setPhone(app.getPhone());
        vo.setIdCard(app.getIdCard());
        vo.setProvince(app.getProvince());
        vo.setCity(app.getCity());
        vo.setDistrict(app.getDistrict());
        vo.setAddress(app.getAddress());
        vo.setCustomerType(app.getCustomerType());
        vo.setOrgName(app.getOrgName());
        vo.setCreditCode(app.getCreditCode());
        vo.setDealerId(app.getDealerId());
        vo.setStatus(app.getStatus());
        vo.setReviewComment(app.getReviewComment());
        vo.setReviewedAt(app.getReviewedAt());
        vo.setCreatedAt(app.getCreatedAt());
        return vo;
    }
}
