package com.platform.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceBindDTO {

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private Long customerId;
}
