package com.platform.ai.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.ai.config.QwenProperties;
import com.platform.ai.dto.QwenAdviceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class QwenClient {

    private final QwenProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public QwenClient(QwenProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    public Optional<QwenAdviceResult> generateAdvice(String prompt) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", """
                                    你是直饮水设备 RO 膜寿命预测模型。只能输出 JSON，不能输出 Markdown。
                                    JSON 字段必须包含 healthScore、riskLevel、estimatedRemainingLiters、estimatedRemainingDays、maintenancePriority、confidence、summary、recommendedActions、reasoning。
                                    riskLevel 只能是 LOW、MEDIUM、HIGH、CRITICAL；healthScore 范围 0-100；remaining 字段必须是非负数字。
                                    预测必须保守、可解释，不能编造未提供的传感器数据。
                                    """),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimRight(properties.getBaseUrl(), "/") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[AI] Qwen request failed, status={}, body={}", response.statusCode(), abbreviate(response.body()));
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (!StringUtils.hasText(content)) {
                return Optional.empty();
            }

            JsonNode adviceJson = objectMapper.readTree(content);
            return Optional.of(QwenAdviceResult.builder()
                    .healthScore(number(adviceJson, "healthScore"))
                    .summary(text(adviceJson, "summary"))
                    .riskLevel(text(adviceJson, "riskLevel"))
                    .estimatedRemainingLiters(number(adviceJson, "estimatedRemainingLiters"))
                    .estimatedRemainingDays(number(adviceJson, "estimatedRemainingDays"))
                    .maintenancePriority(text(adviceJson, "maintenancePriority"))
                    .confidence(text(adviceJson, "confidence"))
                    .recommendedActions(list(adviceJson, "recommendedActions"))
                    .reasoning(text(adviceJson, "reasoning"))
                    .build());
        } catch (Exception ex) {
            log.warn("[AI] Qwen advice generation skipped because model call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public String getModel() {
        return properties.getModel();
    }

    public boolean isConfigured() {
        return properties.isEnabled() && StringUtils.hasText(properties.getApiKey());
    }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            try {
                return Double.parseDouble(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<String> list(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value == null || !value.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(value, new TypeReference<List<String>>() {});
    }

    private static String trimRight(String value, String suffix) {
        String result = value;
        while (result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }

    private static String abbreviate(String body) {
        if (body == null || body.length() <= 300) {
            return body;
        }
        return body.substring(0, 300) + "...";
    }
}
