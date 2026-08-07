package com.platform.push.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.event.DeviceStatusEvent;
import com.platform.common.event.WorkOrderAssignedEvent;
import com.platform.push.config.WechatOfficialProperties;
import com.platform.push.service.OfficialAccountNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialAccountNotificationServiceImpl implements OfficialAccountNotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER_FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 管理员 openid 列表（user-service 通过公众号消息写入） */
    private static final String ADMIN_OPENIDS_KEY = "wechat:official:admin_openids";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final WechatOfficialProperties properties;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    private volatile String cachedAccessToken;
    private volatile LocalDateTime accessTokenExpireAt;

    @Override
    public void notifyDeviceStatus(DeviceStatusEvent event) {
        if (event == null || !StringUtils.hasText(event.getSn())) {
            log.warn("[OfficialAccountNotify] 设备状态事件缺少 SN: {}", event);
            return;
        }

        // 1. 读取已绑定的管理员 openid 列表
        Set<String> adminOpenIds;
        try {
            adminOpenIds = stringRedisTemplate.opsForSet().members(ADMIN_OPENIDS_KEY);
        } catch (Exception e) {
            log.error("[OfficialAccountNotify] 读取管理员 openid 失败", e);
            return;
        }
        if (adminOpenIds == null || adminOpenIds.isEmpty()) {
            log.info("[OfficialAccountNotify] 没有已绑定的管理员，跳过设备上下线通知: sn={}, status={}",
                    event.getSn(), event.getStatus());
            return;
        }

        // 2. 检查模板 ID 配置
        if (!StringUtils.hasText(properties.getDeviceStatusTemplateId())) {
            log.warn("[OfficialAccountNotify] 设备上下线模板 ID 未配置（wechat.official.deviceStatusTemplateId），跳过通知");
            return;
        }

        // 3. 逐个发送模板消息
        for (String openId : adminOpenIds) {
            try {
                sendDeviceStatusTemplateMessage(openId, event);
                log.info("[OfficialAccountNotify] 设备状态通知发送成功: openId={}, sn={}, status={}",
                        maskOpenId(openId), event.getSn(), event.getStatus());
            } catch (Exception e) {
                log.warn("[OfficialAccountNotify] 设备状态通知发送失败: openId={}, sn={}, status={}, err={}",
                        maskOpenId(openId), event.getSn(), event.getStatus(), e.getMessage());
            }
        }
    }

    private void sendDeviceStatusTemplateMessage(String openId, DeviceStatusEvent event) throws Exception {
        if (properties.isMockEnabled()) {
            // mock 模式不实际发送
            return;
        }
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
        Map<String, Object> body = buildDeviceStatusTemplateBody(openId, event);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        String response;
        try {
            response = restTemplate.postForObject(url, request, String.class);
        } catch (HttpStatusCodeException e) {
            String responseBody = defaultText(e.getResponseBodyAsString(), "-");
            throw new IllegalStateException(
                    "WeChat device status template send HTTP failed: status=" + e.getStatusCode().value()
                            + ", body=" + responseBody, e);
        }
        JsonNode json = objectMapper.readTree(response);
        int errCode = json.path("errcode").asInt(-1);
        if (errCode != 0) {
            throw new IllegalStateException("微信设备状态模板发送失败：" + response);
        }
    }

    /**
     * 构建设备上下线模板消息 body
     * 模板字段：工单编号 → deviceId，故障类型 → "上线"/"离线"，推送时间 → occurredAt
     */
    private Map<String, Object> buildDeviceStatusTemplateBody(String openId, DeviceStatusEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openId);
        body.put("template_id", properties.getDeviceStatusTemplateId());

        // 跳转小程序设备详情页（如启用）
        if (properties.isMiniProgramJumpEnabled() && StringUtils.hasText(properties.getMiniProgramAppId())) {
            Map<String, String> miniProgram = new LinkedHashMap<>();
            miniProgram.put("appid", properties.getMiniProgramAppId());
            miniProgram.put("pagepath", buildDeviceMiniProgramPage(event.getDeviceId()));
            body.put("miniprogram", miniProgram);
        }

        String statusText = "ONLINE".equalsIgnoreCase(event.getStatus()) ? "上线" : "离线";
        String deviceId = defaultText(event.getDeviceId(), defaultText(event.getSn(), "-"));
        String occurredAt = event.getOccurredAt() != null
                ? event.getOccurredAt().format(TIME_FORMATTER_FULL)
                : LocalDateTime.now().format(TIME_FORMATTER_FULL);

        Map<String, Object> data = new LinkedHashMap<>();
        putTemplateValue(data, "character_string6", templateCharacterString(deviceId));
        putTemplateValue(data, "thing3", templateThing(statusText));
        putTemplateValue(data, "time15", occurredAt);
        body.put("data", data);
        return body;
    }

    private String buildDeviceMiniProgramPage(String deviceId) {
        String page = properties.getDeviceDetailPage();
        if (!StringUtils.hasText(page)) {
            return "pages/device-detail/device-detail?deviceId=" + deviceId;
        }
        return page.replace("{deviceId}", String.valueOf(deviceId));
    }

    /** character_string 字段：最长 32 字符，保留字母数字中文 */
    private String templateCharacterString(String value) {
        String text = defaultText(value, "-").trim();
        return text.length() <= 32 ? text : text.substring(0, 32);
    }

    @Override
    public void notifyWorkOrderAssigned(WorkOrderAssignedEvent event) {
        if (event == null || event.getOrderId() == null || event.getWorkerId() == null) {
            log.warn("[OfficialAccountNotify] 派单事件缺少必要字段: {}", event);
            return;
        }

        try {
            String officialOpenId = findOfficialOpenId(event.getWorkerId());
            if (!StringUtils.hasText(officialOpenId)) {
                markOrderNotify(event.getOrderId(), "SKIPPED", "运维人员未绑定公众号通知");
                log.info("[OfficialAccountNotify] 运维人员未绑定公众号，跳过通知: orderId={}, workerId={}",
                        event.getOrderId(), event.getWorkerId());
                return;
            }

            if (properties.isMockEnabled()) {
                markOrderNotify(event.getOrderId(), "PUSHED",
                        "mock已发送公众号派单通知：" + maskOpenId(officialOpenId));
                log.info("[OfficialAccountNotify] mock派单通知成功: orderId={}, workerId={}, openId={}",
                        event.getOrderId(), event.getWorkerId(), maskOpenId(officialOpenId));
                return;
            }

            sendTemplateMessage(officialOpenId, event);
            markOrderNotify(event.getOrderId(), "PUSHED", "公众号派单通知发送成功");
            log.info("[OfficialAccountNotify] 公众号派单通知成功: orderId={}, workerId={}",
                    event.getOrderId(), event.getWorkerId());
        } catch (Exception e) {
            markOrderNotify(event.getOrderId(), "FAILED", "公众号派单通知失败：" + e.getMessage());
            log.warn("[OfficialAccountNotify] 公众号派单通知失败: orderId={}, workerId={}",
                    event.getOrderId(), event.getWorkerId(), e);
        }
    }

    private String findOfficialOpenId(Long workerId) {
        List<String> openIds = jdbcTemplate.queryForList(
                "SELECT official_open_id FROM worker_wechat_binding " +
                        "WHERE worker_id = ? AND subscribe_status = 1 AND deleted = 0 " +
                        "ORDER BY bound_at DESC LIMIT 1",
                String.class,
                workerId);
        return openIds.isEmpty() ? null : openIds.get(0);
    }

    private void sendTemplateMessage(String officialOpenId, WorkOrderAssignedEvent event) throws Exception {
        if (!StringUtils.hasText(properties.getWorkOrderAssignedTemplateId())) {
            throw new IllegalStateException("未配置公众号派单模板ID");
        }

        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
        Map<String, Object> body = buildTemplateBody(officialOpenId, event);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        String response;
        try {
            response = restTemplate.postForObject(url, request, String.class);
        } catch (HttpStatusCodeException e) {
            String responseBody = defaultText(e.getResponseBodyAsString(), "-");
            throw new IllegalStateException(
                    "WeChat template send HTTP failed: status=" + e.getStatusCode().value()
                            + ", body=" + responseBody,
                    e);
        }
        JsonNode json = objectMapper.readTree(response);
        int errCode = json.path("errcode").asInt(-1);
        if (errCode != 0) {
            throw new IllegalStateException("微信模板消息发送失败：" + response);
        }
    }

    private String getAccessToken() throws Exception {
        if (StringUtils.hasText(cachedAccessToken)
                && accessTokenExpireAt != null
                && accessTokenExpireAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            return cachedAccessToken;
        }

        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new IllegalStateException("未配置公众号 appId/appSecret");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/cgi-bin/token")
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .build()
                .toUriString();
        String response = restTemplate.getForObject(url, String.class);
        JsonNode json = objectMapper.readTree(response);
        String accessToken = json.path("access_token").asText();
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("获取公众号 access_token 失败：" + response);
        }

        int expiresIn = json.path("expires_in").asInt(7200);
        cachedAccessToken = accessToken;
        accessTokenExpireAt = LocalDateTime.now().plusSeconds(Math.max(300, expiresIn - 300L));
        return accessToken;
    }

    private Map<String, Object> buildTemplateBody(String officialOpenId, WorkOrderAssignedEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", officialOpenId);
        body.put("template_id", properties.getWorkOrderAssignedTemplateId());

        if (properties.isMiniProgramJumpEnabled() && StringUtils.hasText(properties.getMiniProgramAppId())) {
            Map<String, String> miniProgram = new LinkedHashMap<>();
            miniProgram.put("appid", properties.getMiniProgramAppId());
            miniProgram.put("pagepath", buildMiniProgramPage(event.getOrderId()));
            body.put("miniprogram", miniProgram);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        putTemplateValue(data, "first", "您有新的" + defaultText(event.getOrderTypeDesc(), "工单") + "已派发");
        putTemplateValue(data, "keyword1", defaultText(event.getOrderNo(), "-"));
        putTemplateValue(data, "keyword2", defaultText(event.getWorkerName(), "-"));
        putTemplateValue(data, "keyword3", buildDeviceText(event));
        putTemplateValue(data, "keyword4", formatTime(event.getAssignedAt()));
        putTemplateValue(data, "remark", "请进入小程序查看工单详情并及时接单。");
        data.clear();
        putTemplateValue(data, "character_string2", defaultText(event.getOrderNo(), "-"));
        putTemplateValue(data, "thing7", templateThing(resolveNotifyAddress(event)));
        putTemplateValue(data, "time12", formatTime(event.getAssignedAt()));
        putTemplateValue(data, "thing9", templateThing(defaultText(event.getCustomerName(), defaultText(event.getCustomerPhone(), "-"))));
        body.put("data", data);
        return body;
    }

    private void putTemplateValue(Map<String, Object> data, String key, String value) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("value", value);
        data.put(key, item);
    }

    private String buildDeviceText(WorkOrderAssignedEvent event) {
        String device = StringUtils.hasText(event.getDeviceId()) ? event.getDeviceId() : event.getDeviceSn();
        String address = buildAddress(event);
        if (StringUtils.hasText(device) && StringUtils.hasText(address)) {
            return device + " / " + address;
        }
        return defaultText(device, defaultText(address, "-"));
    }

    private String resolveNotifyAddress(WorkOrderAssignedEvent event) {
        String address = buildAddress(event);
        if (StringUtils.hasText(address)) {
            return address;
        }

        address = findAddressByOrderId(event.getOrderId());
        if (StringUtils.hasText(address)) {
            return address;
        }

        return buildDeviceText(event);
    }

    private String findAddressByOrderId(Long orderId) {
        if (orderId == null) {
            return null;
        }
        try {
            List<String> addresses = jdbcTemplate.queryForList(
                    "SELECT CONCAT_WS('', " +
                            "COALESCE(NULLIF(wo.province, ''), NULLIF(c.province, '')), " +
                            "COALESCE(NULLIF(wo.city, ''), NULLIF(c.city, '')), " +
                            "COALESCE(NULLIF(wo.district, ''), NULLIF(c.district, '')), " +
                            "COALESCE(NULLIF(wo.address, ''), NULLIF(c.address, ''))) AS full_address " +
                            "FROM work_order wo LEFT JOIN customer c ON c.id = wo.customer_id AND c.deleted = 0 " +
                            "WHERE wo.id = ? AND wo.deleted = 0",
                    String.class,
                    orderId);
            return addresses.isEmpty() ? null : addresses.get(0);
        } catch (Exception e) {
            log.warn("[OfficialAccountNotify] 查询工单地址失败: orderId={}", orderId, e);
            return null;
        }
    }

    private String buildAddress(WorkOrderAssignedEvent event) {
        StringBuilder builder = new StringBuilder();
        append(builder, event.getProvince());
        append(builder, event.getCity());
        append(builder, event.getDistrict());
        append(builder, event.getAddress());
        return builder.toString();
    }

    private void append(StringBuilder builder, String text) {
        if (StringUtils.hasText(text)) {
            builder.append(text.trim());
        }
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? LocalDateTime.now().format(TIME_FORMATTER) : time.format(TIME_FORMATTER);
    }

    private String buildMiniProgramPage(Long orderId) {
        String page = properties.getWorkOrderDetailPage();
        if (!StringUtils.hasText(page)) {
            return "pages/work-order/detail/index?id=" + orderId;
        }
        return page.replace("{orderId}", String.valueOf(orderId));
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String templateThing(String value) {
        String text = defaultText(value, "-").trim();
        return text.length() <= 20 ? text : text.substring(0, 20);
    }

    private void markOrderNotify(Long orderId, String status, String message) {
        String safeMessage = message != null && message.length() > 256
                ? message.substring(0, 256)
                : message;
        jdbcTemplate.update(
                "UPDATE work_order SET dispatch_notify_status = ?, dispatch_notify_message = ?, dispatch_notified_at = ? WHERE id = ?",
                status, safeMessage, LocalDateTime.now(), orderId);
    }

    private String maskOpenId(String openId) {
        if (!StringUtils.hasText(openId) || openId.length() <= 8) {
            return "****";
        }
        return openId.substring(0, 4) + "****" + openId.substring(openId.length() - 4);
    }
}
