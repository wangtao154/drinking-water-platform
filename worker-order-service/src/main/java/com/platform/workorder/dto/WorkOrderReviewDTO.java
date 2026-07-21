package com.platform.workorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户核查反馈 DTO
 */
@Data
public class WorkOrderReviewDTO {

    /**
     * 评分（1-5）
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer rating;

    /**
     * 核查反馈内容
     */
    private String reviewContent;
}
