package com.platform.device.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceQueryDTO extends PageQueryDTO {

    /** 关键词：支持模糊匹配 deviceId 和 sn */
    private String keyword;

    private String deviceId;

    /** SN 精确/模糊搜索 */
    private String sn;

    /** 型号名称（模糊匹配 device_model.model_name） */
    private String modelName;

    /** 绑定客户名称（通过 Feign 查 user-service 获取 customer IDs） */
    private String customerName;

    /** 激活时间范围 - 开始 */
    private LocalDateTime activatedAtStart;

    /** 激活时间范围 - 结束 */
    private LocalDateTime activatedAtEnd;

    /** 入库时间范围 - 开始 */
    private LocalDateTime createdAtStart;

    /** 入库时间范围 - 结束 */
    private LocalDateTime createdAtEnd;

    private String lifecycleStatus;

    private Integer onlineStatus;

    private Long modelId;

    private Long dealerId;

    private Long customerId;
}
