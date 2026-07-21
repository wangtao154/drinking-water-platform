package com.platform.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DeviceRegisterDTO {

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    @NotBlank(message = "网关序列号不能为空")
    @Pattern(regexp = "^j\\d{6}$", message = "SN格式必须为j+6位数字，如j082438")
    private String sn;

    @NotNull(message = "设备型号ID不能为空")
    private Long modelId;

    private String iccid;

    private String imei;

    private LocalDate productionDate;

    private String productionBatch;
}
