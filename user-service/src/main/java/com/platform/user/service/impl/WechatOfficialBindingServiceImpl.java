package com.platform.user.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.platform.user.config.WechatOfficialProperties;
import com.platform.user.entity.Worker;
import com.platform.user.entity.WorkerWechatBinding;
import com.platform.user.mapper.WorkerMapper;
import com.platform.user.mapper.WorkerWechatBindingMapper;
import com.platform.user.service.WechatOfficialBindingService;
import com.platform.user.vo.OfficialAccountBindStatusVO;
import com.platform.user.vo.OfficialAccountBindUrlVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatOfficialBindingServiceImpl implements WechatOfficialBindingService {

    private static final String STATE_KEY_PREFIX = "wechat:official:bind:";
    private static final String ACCESS_TOKEN_KEY = "wechat:official:access_token";

    private final WorkerMapper workerMapper;
    private final WorkerWechatBindingMapper bindingMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final WechatOfficialProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public OfficialAccountBindUrlVO createBindUrl(Long workerId) {
        Worker worker = requireWorker(workerId);
        WorkerWechatBinding binding = bindingMapper.selectByWorkerId(workerId);
        boolean bound = binding != null && StringUtils.hasText(binding.getOfficialOpenId());

        String state = UUID.randomUUID().toString().replace("-", "");
        int expireSeconds = properties.getBindTokenExpireSeconds() != null
                ? properties.getBindTokenExpireSeconds()
                : 600;
        stringRedisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, worker.getId().toString(), expireSeconds, TimeUnit.SECONDS);

        boolean mock = isMock();
        String bindUrl = mock ? buildMockCallbackUrl(state) : buildWechatOauthUrl(state);

        return OfficialAccountBindUrlVO.builder()
                .bound(bound)
                .bindUrl(bindUrl)
                .mock(mock)
                .expiresInSeconds(expireSeconds)
                .build();
    }

    @Override
    public OfficialAccountBindStatusVO getBindStatus(Long workerId) {
        requireWorker(workerId);
        WorkerWechatBinding binding = bindingMapper.selectByWorkerId(workerId);
        if (binding == null || !StringUtils.hasText(binding.getOfficialOpenId())) {
            return OfficialAccountBindStatusVO.builder()
                    .bound(false)
                    .subscribeStatus(0)
                    .build();
        }
        return OfficialAccountBindStatusVO.builder()
                .bound(true)
                .subscribeStatus(binding.getSubscribeStatus())
                .boundAt(binding.getBoundAt())
                .officialOpenIdMasked(maskOpenId(binding.getOfficialOpenId()))
                .build();
    }

    @Override
    public void handleCallback(String code, String state) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "缺少微信授权参数");
        }

        String key = STATE_KEY_PREFIX + state;
        String workerIdValue = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(workerIdValue)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "绑定链接已过期，请返回小程序重新发起绑定");
        }
        stringRedisTemplate.delete(key);

        Long workerId = Long.valueOf(workerIdValue);
        Worker worker = requireWorker(workerId);
        WechatOauthUser oauthUser = isMock()
                ? mockOauthUser(workerId)
                : requestOauthUser(code);
        WechatOfficialUserInfo userInfo = queryOfficialUserInfo(oauthUser.openId());

        bindWorker(worker, oauthUser, userInfo.subscribeStatus());
        log.info("[WechatOfficialBinding] 公众号绑定成功: workerId={}, officialOpenId={}, subscribeStatus={}",
                workerId, maskOpenId(oauthUser.openId()), userInfo.subscribeStatus());
    }

    @Override
    public String verifyEventCallback(String signature, String timestamp, String nonce, String echostr) {
        if (!isValidSignature(signature, timestamp, nonce)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "公众号事件签名校验失败");
        }
        return echostr;
    }

    @Override
    public void handleEventCallback(String signature, String timestamp, String nonce, String body) {
        if (!isValidSignature(signature, timestamp, nonce)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "公众号事件签名校验失败");
        }
        Map<String, String> event = parseWechatEvent(body);
        String eventType = event.getOrDefault("Event", "").toLowerCase(Locale.ROOT);
        String officialOpenId = event.get("FromUserName");
        if (!StringUtils.hasText(officialOpenId)) {
            log.warn("[WechatOfficialBinding] 公众号事件缺少 FromUserName: {}", event);
            return;
        }

        if ("subscribe".equals(eventType)) {
            updateSubscribeStatus(officialOpenId, 1);
        } else if ("unsubscribe".equals(eventType)) {
            updateSubscribeStatus(officialOpenId, 0);
        } else {
            log.debug("[WechatOfficialBinding] 忽略公众号事件: event={}, openId={}",
                    eventType, maskOpenId(officialOpenId));
        }
    }

    private void bindWorker(Worker worker, WechatOauthUser oauthUser, int subscribeStatus) {
        WorkerWechatBinding officialBinding = bindingMapper.selectByOfficialOpenId(oauthUser.openId());
        WorkerWechatBinding binding = bindingMapper.selectByWorkerId(worker.getId());
        if (officialBinding != null && !officialBinding.getWorkerId().equals(worker.getId())) {
            Worker boundWorker = workerMapper.selectById(officialBinding.getWorkerId());
            if (boundWorker == null) {
                if (binding != null && !binding.getId().equals(officialBinding.getId())) {
                    throw new BusinessException(ResultCode.DATA_DUPLICATE, "当前运维人员已绑定其他公众号账号");
                }
                log.info("[WechatOfficialBinding] 迁移已删除运维人员的公众号绑定: oldWorkerId={}, newWorkerId={}, officialOpenId={}",
                        officialBinding.getWorkerId(), worker.getId(), maskOpenId(oauthUser.openId()));
                officialBinding.setWorkerId(worker.getId());
                officialBinding.setMiniOpenId(worker.getOpenId());
                officialBinding.setOfficialOpenId(oauthUser.openId());
                officialBinding.setUnionId(oauthUser.unionId());
                officialBinding.setSubscribeStatus(subscribeStatus);
                officialBinding.setBoundAt(LocalDateTime.now());
                officialBinding.setUnboundAt(null);
                bindingMapper.updateById(officialBinding);
                return;
            }
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "该公众号账号已绑定其他运维人员");
        }

        if (binding == null) {
            binding = new WorkerWechatBinding();
            binding.setWorkerId(worker.getId());
            binding.setMiniOpenId(worker.getOpenId());
            binding.setOfficialOpenId(oauthUser.openId());
            binding.setUnionId(oauthUser.unionId());
            binding.setSubscribeStatus(subscribeStatus);
            binding.setBoundAt(LocalDateTime.now());
            bindingMapper.insert(binding);
        } else {
            binding.setMiniOpenId(worker.getOpenId());
            binding.setOfficialOpenId(oauthUser.openId());
            binding.setUnionId(oauthUser.unionId());
            binding.setSubscribeStatus(subscribeStatus);
            binding.setBoundAt(LocalDateTime.now());
            binding.setUnboundAt(null);
            bindingMapper.updateById(binding);
        }
    }

    private WechatOauthUser requestOauthUser(String code) {
        try {
            String url = "https://api.weixin.qq.com/sns/oauth2/access_token"
                    + "?appid=" + encode(properties.getAppId())
                    + "&secret=" + encode(properties.getAppSecret())
                    + "&code=" + encode(code)
                    + "&grant_type=authorization_code";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("errcode")) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "微信授权失败: " + root.path("errmsg").asText("unknown"));
            }
            String openId = root.path("openid").asText("");
            if (!StringUtils.hasText(openId)) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "微信授权未返回 openid");
            }
            String unionId = root.path("unionid").asText(null);
            return new WechatOauthUser(openId, unionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WechatOfficialBinding] 微信 OAuth 请求失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "微信授权请求失败");
        }
    }

    private WechatOfficialUserInfo queryOfficialUserInfo(String officialOpenId) {
        if (!StringUtils.hasText(officialOpenId)) {
            return new WechatOfficialUserInfo(0);
        }
        if (isMock()) {
            return new WechatOfficialUserInfo(1);
        }
        try {
            return requestOfficialUserInfo(officialOpenId, false);
        } catch (Exception e) {
            log.warn("[WechatOfficialBinding] 查询公众号关注状态失败，绑定继续但 subscribeStatus 保持 0: openId={}, error={}",
                    maskOpenId(officialOpenId), e.getMessage());
            return new WechatOfficialUserInfo(0);
        }
    }

    private WechatOfficialUserInfo requestOfficialUserInfo(String officialOpenId, boolean retried) throws Exception {
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/user/info"
                + "?access_token=" + encode(accessToken)
                + "&openid=" + encode(officialOpenId)
                + "&lang=zh_CN";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("errcode")) {
            int errCode = root.path("errcode").asInt();
            if (!retried && (errCode == 40001 || errCode == 42001)) {
                stringRedisTemplate.delete(ACCESS_TOKEN_KEY);
                return requestOfficialUserInfo(officialOpenId, true);
            }
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "微信用户信息查询失败: " + root.path("errmsg").asText("unknown"));
        }
        return new WechatOfficialUserInfo(root.path("subscribe").asInt(0) == 1 ? 1 : 0);
    }

    private String getAccessToken() throws Exception {
        String cached = stringRedisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        String url = "https://api.weixin.qq.com/cgi-bin/token"
                + "?grant_type=client_credential"
                + "&appid=" + encode(properties.getAppId())
                + "&secret=" + encode(properties.getAppSecret());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("errcode")) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "微信 access_token 获取失败: " + root.path("errmsg").asText("unknown"));
        }

        String accessToken = root.path("access_token").asText("");
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "微信 access_token 为空");
        }
        long expiresIn = Math.max(60, root.path("expires_in").asLong(7200) - 300);
        stringRedisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, accessToken, expiresIn, TimeUnit.SECONDS);
        return accessToken;
    }

    private void updateSubscribeStatus(String officialOpenId, int subscribeStatus) {
        WorkerWechatBinding binding = bindingMapper.selectByOfficialOpenId(officialOpenId);
        if (binding == null) {
            log.info("[WechatOfficialBinding] 收到公众号关注状态事件，但未找到绑定记录: openId={}, subscribeStatus={}",
                    maskOpenId(officialOpenId), subscribeStatus);
            return;
        }
        binding.setSubscribeStatus(subscribeStatus);
        bindingMapper.updateById(binding);
        log.info("[WechatOfficialBinding] 已更新公众号关注状态: workerId={}, openId={}, subscribeStatus={}",
                binding.getWorkerId(), maskOpenId(officialOpenId), subscribeStatus);
    }

    private boolean isValidSignature(String signature, String timestamp, String nonce) {
        if (!StringUtils.hasText(properties.getServerToken())) {
            log.warn("[WechatOfficialBinding] WX_OFFICIAL_SERVER_TOKEN 未配置，拒绝公众号事件回调");
            return false;
        }
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
            return false;
        }
        try {
            List<String> parts = new ArrayList<>();
            parts.add(properties.getServerToken());
            parts.add(timestamp);
            parts.add(nonce);
            parts.sort(String::compareTo);
            String text = String.join("", parts);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("[WechatOfficialBinding] 公众号事件签名计算失败", e);
            return false;
        }
    }

    private Map<String, String> parseWechatEvent(String body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
            Element root = document.getDocumentElement();
            NodeList children = root.getChildNodes();
            Map<String, String> result = new java.util.HashMap<>();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element child) {
                    result.put(child.getTagName(), child.getTextContent());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[WechatOfficialBinding] 公众号事件 XML 解析失败: {}", body, e);
            throw new BusinessException(ResultCode.PARAM_INVALID, "公众号事件解析失败");
        }
    }

    private Worker requireWorker(Long workerId) {
        if (workerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录状态无效");
        }
        Worker worker = workerMapper.selectById(workerId);
        if (worker == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "运维人员不存在");
        }
        return worker;
    }

    private String buildWechatOauthUrl(String state) {
        String callbackUrl = getCallbackUrl();
        return "https://open.weixin.qq.com/connect/oauth2/authorize"
                + "?appid=" + encode(properties.getAppId())
                + "&redirect_uri=" + encode(callbackUrl)
                + "&response_type=code"
                + "&scope=snsapi_base"
                + "&state=" + encode(state)
                + "#wechat_redirect";
    }

    private String buildMockCallbackUrl(String state) {
        String separator = getCallbackUrl().contains("?") ? "&" : "?";
        return getCallbackUrl() + separator + "code=mock_code&state=" + encode(state);
    }

    private String getCallbackUrl() {
        if (StringUtils.hasText(properties.getOauthCallbackUrl())) {
            return properties.getOauthCallbackUrl();
        }
        return "http://localhost:8082/api/v1/wechat/official/callback";
    }

    private boolean isMock() {
        return Boolean.TRUE.equals(properties.getMock())
                || !StringUtils.hasText(properties.getAppId())
                || properties.getAppId().startsWith("mock");
    }

    private WechatOauthUser mockOauthUser(Long workerId) {
        return new WechatOauthUser("mock_official_openid_worker_" + workerId, null);
    }

    private String maskOpenId(String openId) {
        if (!StringUtils.hasText(openId) || openId.length() <= 10) {
            return openId;
        }
        return openId.substring(0, 6) + "****" + openId.substring(openId.length() - 4);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private record WechatOauthUser(String openId, String unionId) {
    }

    private record WechatOfficialUserInfo(int subscribeStatus) {
    }
}
