package com.platform.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.user.dto.ApplicationCreateDTO;
import com.platform.user.dto.ApplicationQueryDTO;
import com.platform.user.dto.ApplicationReviewDTO;
import com.platform.user.entity.Application;
import com.platform.user.vo.ApplicationVO;

public interface ApplicationService {

    /**
     * 游客提交身份申请
     */
    Application create(Long applicantId, ApplicationCreateDTO dto);

    /**
     * 查询自己的申请列表
     */
    java.util.List<ApplicationVO> myApplications(Long applicantId);

    /**
     * 分页查询申请列表（PC端）
     */
    IPage<ApplicationVO> page(ApplicationQueryDTO query);

    /**
     * 查看申请详情
     */
    Application getById(Long id);

    /**
     * 审批申请（通过/驳回）
     */
    Application review(Long id, ApplicationReviewDTO dto, Long reviewerId);
    /**
     * 删除申请
     */
    void delete(Long id);
}
