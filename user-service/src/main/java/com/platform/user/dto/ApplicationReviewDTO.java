package com.platform.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 申请审批 DTO（PC端管理员操作）
 */
@Data
public class ApplicationReviewDTO {

    /** 是否通过 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 审批意见 */
    private String reviewComment;
}
