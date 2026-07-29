package com.platform.water.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WechatPayParamsVO {

    private String appId;

    private String timeStamp;

    private String nonceStr;

    @JsonProperty("package")
    private String packageValue;

    private String signType;

    private String paySign;
}
