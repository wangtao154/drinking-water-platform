package com.platform.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.ai.config.QwenProperties;
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

/**
 * General Qwen chat client used by the isolated assistant only.
 * RO prediction keeps its strict JSON contract in QwenClient.
 */
@Slf4j
@Component
public class QwenChatClient {

    private final QwenProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public QwenChatClient(QwenProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    public Optional<String> chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "temperature", 0.1,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
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
                log.warn("[Assistant] Qwen request failed, status={}", response.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            return StringUtils.hasText(content) ? Optional.of(content) : Optional.empty();
        } catch (Exception ex) {
            log.warn("[Assistant] Qwen chat unavailable: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean isConfigured() {
        return properties.isEnabled() && StringUtils.hasText(properties.getApiKey());
    }

    public String getModel() {
        return properties.getModel();
    }

    private static String trimRight(String value, String suffix) {
        String result = value;
        while (result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }
}
