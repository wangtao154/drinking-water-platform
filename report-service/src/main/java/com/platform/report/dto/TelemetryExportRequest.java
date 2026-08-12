package com.platform.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TelemetryExportRequest {

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /** 当前控制板 SN，仅用于导出元数据展示，不参与遥测归属查询。 */
    private String sn;

    @NotBlank(message = "导出点位不能为空")
    private String fields;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    private String interval = "10m";
}
