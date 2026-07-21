package com.platform.workorder.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 开始作业 DTO
 */
@Data
public class WorkOrderStartDTO implements Serializable {

    /**
     * 作业前照片URL列表
     */
    private List<String> beforePhotos;
}
