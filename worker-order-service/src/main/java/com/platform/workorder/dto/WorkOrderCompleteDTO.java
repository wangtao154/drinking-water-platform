package com.platform.workorder.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 完成工单 DTO
 */
@Data
public class WorkOrderCompleteDTO implements Serializable {

    /**
     * 作业后照片URL列表
     */
    private List<String> afterPhotos;

    /**
     * 完成备注
     */
    private String remark;

    /**
     * 客户评分（1-5）
     */
    private Integer rating;

    /**
     * 客户评价内容
     */
    private String reviewContent;

    /**
     * 旧滤芯ID（滤芯更换工单用）
     */
    private String oldFilterId;

    /**
     * 新滤芯ID（滤芯更换工单用）
     */
    private String newFilterId;
}
