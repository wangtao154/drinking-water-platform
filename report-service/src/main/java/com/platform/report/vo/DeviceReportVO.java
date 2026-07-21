package com.platform.report.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备统计 VO
 */
@Data
public class DeviceReportVO implements Serializable {

    /**
     * 设备总数
     */
    private Long totalDevices;

    /**
     * 在线数量
     */
    private Long onlineCount;

    /**
     * 离线数量
     */
    private Long offlineCount;

    /**
     * 故障数量
     */
    private Long faultCount;

    /**
     * 今日注册数量
     */
    private Long registeredToday;

    /**
     * 今日激活数量
     */
    private Long activatedToday;
}
