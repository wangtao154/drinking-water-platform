package com.platform.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.auth.UserContext;
import com.platform.common.base.BaseController;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.user.dto.ApplicationCreateDTO;
import com.platform.user.dto.ApplicationQueryDTO;
import com.platform.user.dto.ApplicationReviewDTO;
import com.platform.user.entity.Application;
import com.platform.user.service.ApplicationService;
import com.platform.user.vo.ApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户身份申请 API
 *
 * 小程序端：
 *   POST /api/v1/applications        游客提交申请
 *   GET  /api/v1/applications/my     查询自己的申请记录
 *
 * PC端：
 *   GET  /api/v1/applications        分页查询申请列表
 *   GET  /api/v1/applications/{id}   查看申请详情
 *   PUT  /api/v1/applications/{id}/review  审批（通过/驳回）
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController extends BaseController {

    private final ApplicationService applicationService;

    /**
     * 游客提交身份申请
     */
    @PostMapping
    public R<Application> create(@Valid @RequestBody ApplicationCreateDTO dto) {
        Long applicantId = UserContext.getUserId();
        return success(applicationService.create(applicantId, dto));
    }

    /**
     * 查询自己的申请记录（小程序端）
     */
    @GetMapping("/my")
    public R<List<ApplicationVO>> myApplications() {
        Long applicantId = UserContext.getUserId();
        return success(applicationService.myApplications(applicantId));
    }

    /**
     * 分页查询申请列表（PC端）
     */
    @GetMapping
    public R<PageResult<ApplicationVO>> page(ApplicationQueryDTO query) {
        IPage<ApplicationVO> page = applicationService.page(query);
        return pageResult(PageResult.of(page));
    }

    /**
     * 查看申请详情
     */
    @GetMapping("/{id}")
    public R<Application> getById(@PathVariable Long id) {
        return success(applicationService.getById(id));
    }

    /**
     * 删除申请（PC端管理员操作）
     */
    /**
     * 审批申请（PC端）
     */
    @PutMapping("/{id}/review")
    public R<Application> review(@PathVariable Long id, @Valid @RequestBody ApplicationReviewDTO dto) {
        Long reviewerId = UserContext.getUserId();
        return success(applicationService.review(id, dto, reviewerId));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return success(null);
    }
}
