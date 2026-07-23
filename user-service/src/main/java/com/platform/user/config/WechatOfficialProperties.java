package com.platform.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.official")
public class WechatOfficialProperties {

    private String appId = "";

    private String appSecret = "";

    private String oauthCallbackUrl = "";

    private Boolean mock = true;

    private Integer bindTokenExpireSeconds = 600;

    /** 公众号服务器配置 Token，用于校验微信事件回调签名 */
    private String serverToken = "";
}
