package com.platform.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.platform.ai.config.QwenProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
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

    /** One function call requested by the model: tool name plus raw JSON arguments. */
    public record ToolCall(String name, String argumentsJson) {
    }

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

    /**
     * Tool-calling round: the model receives the fixed tool schemas and decides
     * which tools to invoke with which arguments. The model never sees URLs or
     * credentials — it can only pick a whitelisted name and fill parameters.
     *
     * @return empty when Qwen is unavailable or the model chose no tool.
     */
    public Optional<List<ToolCall>> chatWithTools(String systemPrompt, String userPrompt, String toolsJson) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", properties.getModel());
            body.put("temperature", 0.1);
            ArrayNode messages = body.putArray("messages");
            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            body.set("tools", objectMapper.readTree(toolsJson));
            body.put("tool_choice", "auto");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimRight(properties.getBaseUrl(), "/") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[Assistant] Qwen tools request failed, status={}", response.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode toolCalls = root.path("choices").path(0).path("message").path("tool_calls");
            if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                return Optional.empty();
            }
            List<ToolCall> calls = new ArrayList<>();
            for (JsonNode call : toolCalls) {
                String name = call.path("function").path("name").asText("");
                String arguments = call.path("function").path("arguments").asText("{}");
                if (StringUtils.hasText(name)) {
                    calls.add(new ToolCall(name, arguments));
                }
            }
            return calls.isEmpty() ? Optional.empty() : Optional.of(calls);
        } catch (Exception ex) {
            log.warn("[Assistant] Qwen tool-call unavailable: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static String trimRight(String value, String suffix) {
        String result = value;
        while (result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }
}
