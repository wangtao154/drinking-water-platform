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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatOfficialBindingServiceImpl implements WechatOfficialBindingService {

    private static final String STATE_KEY_PREFIX = "wechat:official:bind:";

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

        bindWorker(worker, oauthUser);
        log.info("[WechatOfficialBinding] 公众号绑定成功: workerId={}, officialOpenId={}",
                workerId, maskOpenId(oauthUser.openId()));
    }

    private void bindWorker(Worker worker, WechatOauthUser oauthUser) {
        WorkerWechatBinding officialBinding = bindingMapper.selectByOfficialOpenId(oauthUser.openId());
        if (officialBinding != null && !officialBinding.getWorkerId().equals(worker.getId())) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "该公众号账号已绑定其他运维人员");
        }

        WorkerWechatBinding binding = bindingMapper.selectByWorkerId(worker.getId());
        if (binding == null) {
            binding = new WorkerWechatBinding();
            binding.setWorkerId(worker.getId());
            binding.setMiniOpenId(worker.getOpenId());
            binding.setOfficialOpenId(oauthUser.openId());
            binding.setUnionId(oauthUser.unionId());
            binding.setSubscribeStatus(0);
            binding.setBoundAt(LocalDateTime.now());
            bindingMapper.insert(binding);
        } else {
            binding.setMiniOpenId(worker.getOpenId());
            binding.setOfficialOpenId(oauthUser.openId());
            binding.setUnionId(oauthUser.unionId());
            binding.setSubscribeStatus(binding.getSubscribeStatus() != null ? binding.getSubscribeStatus() : 0);
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
}
