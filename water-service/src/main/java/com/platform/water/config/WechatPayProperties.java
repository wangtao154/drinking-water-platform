package com.platform.water.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {

    private Boolean enabled = false;

    private String appId;

    private String mchId;

    private String apiV3Key;

    private String certSerialNo;

    private String privateKeyPath;

    private String platformPublicKeyId;

    private String platformPublicKeyPath;

    private String notifyUrl;

    private String apiBaseUrl = "https://api.mch.weixin.qq.com";
}
