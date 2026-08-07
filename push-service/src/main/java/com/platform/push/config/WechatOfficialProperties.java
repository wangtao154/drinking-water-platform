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

    /** 设备上下线通知模板 ID（工单编号=设备ID、故障类型=上/下线、推送时间=变更时间） */
    private String deviceStatusTemplateId;

    private String miniProgramAppId;

    private Boolean miniProgramJumpEnabled = false;

    private String workOrderDetailPage = "pages/work-order/detail/index?id={orderId}";

    /** 设备详情小程序页面（设备上下线通知点击跳转） */
    private String deviceDetailPage = "pages/device-detail/device-detail?deviceId={deviceId}";

    public boolean isMockEnabled() {
        return Boolean.TRUE.equals(mock);
    }

    public boolean isMiniProgramJumpEnabled() {
        return Boolean.TRUE.equals(miniProgramJumpEnabled);
    }
}
