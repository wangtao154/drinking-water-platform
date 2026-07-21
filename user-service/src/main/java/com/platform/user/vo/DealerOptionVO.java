package com.platform.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 经销商选项 VO（小程序选择器用，仅返回 id + 名称）
 */
@Data
public class DealerOptionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String dealerName;

    private String dealerCode;
}
