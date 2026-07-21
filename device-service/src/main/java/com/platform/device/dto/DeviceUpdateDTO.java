package com.platform.device.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DeviceUpdateDTO {

    private Long modelId;

    @Pattern(regexp = "^j\\d{6}$", message = "SN格式必须为j+6位数字，如j082438")
    private String sn;

    private String iccid;

    private String imei;

    private String remark;
}
