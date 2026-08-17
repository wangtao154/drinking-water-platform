package com.platform.water.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.platform.water.config.WechatPayProperties;
import com.platform.water.vo.WechatPayParamsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPayClient {

    private static final String AUTH_SCHEMA = "WECHATPAY2-SHA256-RSA2048";
    private static final String JSAPI_PATH = "/v3/pay/transactions/jsapi";
    private static final String REFUND_PATH = "/v3/refund/domestic/refunds";
    private static final String CERTIFICATES_PATH = "/v3/certificates";

    private final WechatPayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, X509Certificate> platformCertificates = new ConcurrentHashMap<>();

    private volatile PrivateKey merchantPrivateKey;
    private volatile PublicKey configuredPlatformPublicKey;

    public String createJsapiPrepay(String orderNo, String description, Long amount, String openId) {
        assertConfigured();
        if (amount == null || amount < 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay amount must be positive");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appid", properties.getAppId());
            body.put("mchid", properties.getMchId());
            body.put("description", description);
            body.put("out_trade_no", orderNo);
            body.put("notify_url", properties.getNotifyUrl());
            body.put("amount", Map.of("total", BigInteger.valueOf(amount), "currency", "CNY"));
            body.put("payer", Map.of("openid", openId));

            String bodyJson = objectMapper.writeValueAsString(body);
            HttpRequest request = signedRequest("POST", JSAPI_PATH, bodyJson)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[WechatPay] JSAPI prepay failed: status={}, body={}",
                        response.statusCode(), sanitizeResponseBody(response.body()));
                throw new BusinessException(ResultCode.PAYMENT_FAILED, "wechat pay prepay failed");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String prepayId = root.path("prepay_id").asText("");
            if (!StringUtils.hasText(prepayId)) {
                throw new BusinessException(ResultCode.PAYMENT_FAILED, "wechat pay prepay_id is empty");
            }
            return prepayId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WechatPay] JSAPI prepay error", e);
            throw new BusinessException(ResultCode.PAYMENT_FAILED, "wechat pay prepay error: " + e.getMessage());
        }
    }

    public WechatPayParamsVO buildMiniProgramPayParams(String prepayId) {
        assertConfigured();
        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String packageValue = "prepay_id=" + prepayId;
        String message = properties.getAppId() + "\n"
                + timeStamp + "\n"
                + nonceStr + "\n"
                + packageValue + "\n";

        WechatPayParamsVO vo = new WechatPayParamsVO();
        vo.setAppId(properties.getAppId());
        vo.setTimeStamp(timeStamp);
        vo.setNonceStr(nonceStr);
        vo.setPackageValue(packageValue);
        vo.setSignType("RSA");
        vo.setPaySign(sign(message));
        return vo;
    }

    public JsonNode createRefund(String orderNo, String refundNo, Long refundAmount, Long totalAmount, String reason) {
        assertConfigured();
        if (refundAmount == null || refundAmount < 1 || totalAmount == null || totalAmount < 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat refund amount must be positive");
        }
        if (!StringUtils.hasText(properties.getRefundNotifyUrl())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat refund notify url is empty");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("out_trade_no", orderNo);
            body.put("out_refund_no", refundNo);
            body.put("reason", StringUtils.hasText(reason) ? reason : "scan water dispense abnormal");
            body.put("notify_url", properties.getRefundNotifyUrl());
            body.put("amount", Map.of(
                    "refund", BigInteger.valueOf(refundAmount),
                    "total", BigInteger.valueOf(totalAmount),
                    "currency", "CNY"));

            String bodyJson = objectMapper.writeValueAsString(body);
            HttpRequest request = signedRequest("POST", REFUND_PATH, bodyJson)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[WechatPay] refund failed: status={}, body={}",
                        response.statusCode(), sanitizeResponseBody(response.body()));
                throw new BusinessException(ResultCode.PAYMENT_FAILED, "wechat refund failed");
            }
            return objectMapper.readTree(response.body());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WechatPay] refund error", e);
            throw new BusinessException(ResultCode.PAYMENT_FAILED, "wechat refund error: " + e.getMessage());
        }
    }

    public JsonNode queryRefund(String refundNo) {
        assertConfigured();
        if (!StringUtils.hasText(refundNo)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat refund no is empty");
        }
        try {
            HttpRequest request = signedRequest("GET", REFUND_PATH + "/" + refundNo, "")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[WechatPay] refund query failed: status={}, body={}",
                        response.statusCode(), sanitizeResponseBody(response.body()));
                throw new BusinessException(ResultCode.PAYMENT_FAILED, "wechat refund query failed");
            }
            return objectMapper.readTree(response.body());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WechatPay] refund query error", e);
            throw new BusinessException(ResultCode.PAYMENT_FAILED, "wechat refund query error: " + e.getMessage());
        }
    }

    public JsonNode decryptAndVerifyNotify(String timestamp, String nonce, String signature, String serial, String body) {
        assertConfigured();
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature) || !StringUtils.hasText(serial)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify headers missing");
        }

        try {
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(resolveWechatPayPublicKey(serial));
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signature))) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify signature invalid");
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode resource = root.path("resource");
            String decrypted = decryptResource(
                    resource.path("associated_data").asText(""),
                    resource.path("nonce").asText(""),
                    resource.path("ciphertext").asText(""));
            return objectMapper.readTree(decrypted);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WechatPay] notify verify/decrypt error", e);
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify error: " + e.getMessage());
        }
    }

    private void refreshPlatformCertificates() throws Exception {
        HttpRequest request = signedRequest("GET", CERTIFICATES_PATH, "")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("[WechatPay] download platform certificates failed: status={}, body={}",
                    response.statusCode(), sanitizeResponseBody(response.body()));
            throw new BusinessException(ResultCode.PAYMENT_FAILED, "download wechat pay platform certificates failed");
        }

        JsonNode root = objectMapper.readTree(response.body());
        for (JsonNode item : root.path("data")) {
            String serialNo = item.path("serial_no").asText("");
            JsonNode encryptCertificate = item.path("encrypt_certificate");
            String plainCert = decryptResource(
                    encryptCertificate.path("associated_data").asText(""),
                    encryptCertificate.path("nonce").asText(""),
                    encryptCertificate.path("ciphertext").asText(""));
            platformCertificates.put(serialNo, parseCertificate(plainCert));
        }
        log.info("[WechatPay] platform certificates refreshed, count={}", platformCertificates.size());
    }

    private PublicKey resolveWechatPayPublicKey(String serial) throws Exception {
        if (StringUtils.hasText(properties.getPlatformPublicKeyId())
                && StringUtils.hasText(properties.getPlatformPublicKeyPath())) {
            if (!properties.getPlatformPublicKeyId().equals(serial)) {
                log.warn("[WechatPay] notify serial does not match configured platform public key id: serial={}, configured={}",
                        serial, properties.getPlatformPublicKeyId());
            } else {
                return loadConfiguredPlatformPublicKey();
            }
        }

        X509Certificate certificate = platformCertificates.get(serial);
        if (certificate == null) {
            refreshPlatformCertificates();
            certificate = platformCertificates.get(serial);
        }
        if (certificate == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay platform certificate not found: " + serial);
        }
        return certificate.getPublicKey();
    }

    private HttpRequest.Builder signedRequest(String method, String path, String body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonceStr + "\n" + body + "\n";
        String signature = sign(message);
        String authorization = AUTH_SCHEMA
                + " mchid=\"" + properties.getMchId() + "\","
                + "nonce_str=\"" + nonceStr + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + properties.getCertSerialNo() + "\","
                + "signature=\"" + signature + "\"";

        return HttpRequest.newBuilder(URI.create(properties.getApiBaseUrl() + path))
                .header("Accept", "application/json")
                .header("Authorization", authorization)
                .header("User-Agent", "drinking-water-platform/1.0");
    }

    private String sign(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAYMENT_FAILED, "wechat pay sign failed: " + e.getMessage());
        }
    }

    private String decryptResource(String associatedData, String nonce, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(properties.getApiV3Key().getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay resource decrypt failed: " + e.getMessage());
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {
        if (merchantPrivateKey != null) {
            return merchantPrivateKey;
        }
        String pem = Files.readString(Path.of(properties.getPrivateKeyPath()), StandardCharsets.UTF_8);
        pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem));
        merchantPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        return merchantPrivateKey;
    }

    private PublicKey loadConfiguredPlatformPublicKey() throws Exception {
        if (configuredPlatformPublicKey != null) {
            return configuredPlatformPublicKey;
        }
        String pem = Files.readString(Path.of(properties.getPlatformPublicKeyPath()), StandardCharsets.UTF_8);
        pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pem));
        configuredPlatformPublicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
        return configuredPlatformPublicKey;
    }

    private X509Certificate parseCertificate(String pem) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay platform certificate parse failed: " + e.getMessage());
        }
    }

    private void assertConfigured() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay disabled");
        }
        if (!StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getMchId())
                || !StringUtils.hasText(properties.getApiV3Key())
                || !StringUtils.hasText(properties.getCertSerialNo())
                || !StringUtils.hasText(properties.getPrivateKeyPath())
                || !StringUtils.hasText(properties.getNotifyUrl())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay config incomplete");
        }
        if (properties.getApiV3Key().getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay api v3 key must be 32 bytes");
        }
        if (StringUtils.hasText(properties.getPlatformPublicKeyId())
                != StringUtils.hasText(properties.getPlatformPublicKeyPath())) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                    "wechat pay platform public key id and path must be configured together");
        }
    }

    private String sanitizeResponseBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        String sanitized = body
                .replaceAll("(?i)(openid|transaction_id|prepay_id|out_trade_no|out_refund_no|refund_id)\"\\s*:\\s*\"[^\"]+\"", "$1\":\"***\"")
                .replaceAll("(?i)(signature|ciphertext)\"\\s*:\\s*\"[^\"]+\"", "$1\":\"***\"");
        return sanitized.substring(0, Math.min(sanitized.length(), 512));
    }
}
