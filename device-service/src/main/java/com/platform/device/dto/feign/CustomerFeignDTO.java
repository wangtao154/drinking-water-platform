package com.platform.device.dto.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户信息 Feign DTO（来自 user-service）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerFeignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String phone;

    private String customerType;

    private String status;
}
