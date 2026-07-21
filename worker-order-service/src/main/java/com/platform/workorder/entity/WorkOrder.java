package com.platform.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity {

    /**
     * 工单编号（唯一）
     */
    private String orderNo;

    /**
     * 工单类型：INSTALL_APPOINTMENT/REPAIR/REMOVE/RELOCATE/FILTER_REPLACE
     */
    private String orderType;

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
     * 客户提交的图片URL列表（JSON数组）
     */
    private String customerImages;

    /**
     * 设备型号ID
     */
    private Long modelId;

    /**
     * 经销商ID
     */
    private Long dealerId;

    /**
     * 指派运维人员ID
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
     * 工单状态：PENDING/ASSIGNED/ACCEPTED/IN_PROGRESS/COMPLETED/CANCELLED
     */
    private String orderStatus;

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
     * 完成照片URL列表（JSON）
     */
    private String photoUrls;

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
     * 旧滤芯ID
     */
    private String oldFilterId;

    /**
     * 新滤芯ID
     */
    private String newFilterId;

    /**
     * 触发方式：AUTO/MANUAL
     */
    private String triggerType;

    /**
     * 派单公众号通知状态：NONE/PENDING/PUSHED/SKIPPED/FAILED
     */
    private String dispatchNotifyStatus;

    /**
     * 派单公众号通知结果说明
     */
    private String dispatchNotifyMessage;

    /**
     * 派单公众号通知处理时间
     */
    private LocalDateTime dispatchNotifiedAt;
}
