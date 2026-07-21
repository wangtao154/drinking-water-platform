package com.platform.workorder.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 VO
 */
@Data
public class WorkOrderVO implements Serializable {

    private Long id;

    /**
     * 工单编号
     */
    private String orderNo;

    /**
     * 工单类型
     */
    private String orderType;

    /**
     * 工单类型描述
     */
    private String orderTypeDesc;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备序列号
     */
    private String deviceSn;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 客户电话
     */
    private String customerPhone;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 设备地址
     */
    private String address;

    /**
     * 工单描述
     */
    private String description;

    /**
     * 客户提交的图片URL列表
     */
    private List<String> customerImages;

    /**
     * 设备型号ID
     */
    private Long modelId;

    /**
     * 经销商ID
     */
    private Long dealerId;

    /**
     * 运维人员ID
     */
    private Long workerId;

    /**
     * 运维人员姓名
     */
    private String workerName;

    /**
     * 运维人员电话
     */
    private String workerPhone;

    /**
     * 工单状态
     */
    private String orderStatus;

    /**
     * 工单状态描述
     */
    private String orderStatusDesc;

    /**
     * 预约时间
     */
    private LocalDateTime appointTime;

    /**
     * 接单时间
     */
    private LocalDateTime acceptedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 取消时间
     */
    private LocalDateTime cancelledAt;

    /**
     * 完成照片URL列表
     */
    private List<String> photoUrls;

    /**
     * 完成备注
     */
    private String remark;

    /**
     * 客户评分
     */
    private Integer rating;

    /**
     * 客户评价内容
     */
    private String reviewContent;

    /**
     * 旧滤芯ID
     */
    private String oldFilterId;

    /**
     * 新滤芯ID
     */
    private String newFilterId;

    /**
     * 触发方式
     */
    private String triggerType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 耗时（小时，完成工单才有值）
     */
    private Long durationHours;

    /**
     * 状态流转日志列表
     */
    private List<WorkOrderLogVO> statusLogs;
}
