package com.platform.push.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.official")
public class WechatOfficialProperties {

    private String appId;

    private String appSecret;

    private Boolean mock = true;

    private String workOrderAssignedTemplateId;

    private String miniProgramAppId;

    private Boolean miniProgramJumpEnabled = false;

    private String workOrderDetailPage = "pages/work-order/detail/index?id={orderId}";

    public boolean isMockEnabled() {
        return Boolean.TRUE.equals(mock);
    }

    public boolean isMiniProgramJumpEnabled() {
        return Boolean.TRUE.equals(miniProgramJumpEnabled);
    }
}
