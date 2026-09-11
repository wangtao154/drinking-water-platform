package com.platform.ai.controller;

import com.platform.ai.assistant.dto.AdminAssistantChatRequest;
import com.platform.ai.assistant.dto.AdminAssistantChatResponse;
import com.platform.ai.assistant.external.AdminAssistantApiKeyService;
import com.platform.ai.assistant.external.ExternalAssistantApiKeyPrincipal;
import com.platform.ai.assistant.external.ExternalAssistantApiRateLimiter;
import com.platform.ai.assistant.service.AdminAssistantService;
import com.platform.common.auth.UserContext;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * External, API-key authenticated facade for the controlled read-only assistant.
 * It deliberately exposes no device-control, finance-write, account or role APIs.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/open")
public class OpenAssistantApiController {

    private final AdminAssistantApiKeyService apiKeyService;
    private final ExternalAssistantApiRateLimiter rateLimiter;
    private final AdminAssistantService adminAssistantService;

    @PostMapping("/chat")
    public R<AdminAssistantChatResponse> chat(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody AdminAssistantChatRequest request,
            HttpServletRequest servletRequest) {
        long startedAt = System.currentTimeMillis();
        ExternalAssistantApiKeyPrincipal principal = null;
        String clientIp = clientIp(servletRequest);
        String requestId = null;
        String status = "ERROR";
        int responseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        try {
            principal = apiKeyService.authenticate(apiKey, clientIp);
            rateLimiter.check(principal.apiKeyId(), principal.rateLimitPerMinute());
            UserContext.set(principal.user());
            AdminAssistantChatResponse response = adminAssistantService.chat(request);
            requestId = response.getRequestId();
            status = response.isFallback() ? "FALLBACK" : "SUCCESS";
            responseCode = HttpStatus.OK.value();
            return R.ok(response);
        } catch (BusinessException ex) {
            status = "REJECTED";
            responseCode = statusCode(ex);
            throw ex;
        } finally {
            if (principal != null) {
                apiKeyService.recordAccess(principal, requestId, status, responseCode,
                        System.currentTimeMillis() - startedAt, clientIp);
            } else {
                apiKeyService.recordRejectedAccess(apiKey, responseCode,
                        System.currentTimeMillis() - startedAt, clientIp);
            }
            UserContext.clear();
        }
    }

    private static int statusCode(BusinessException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("请求过于频繁")) {
            return HttpStatus.TOO_MANY_REQUESTS.value();
        }
        return switch (ex.getCode()) {
            case 40100 -> HttpStatus.UNAUTHORIZED.value();
            case 40300 -> HttpStatus.FORBIDDEN.value();
            case 40400 -> HttpStatus.NOT_FOUND.value();
            default -> HttpStatus.BAD_REQUEST.value();
        };
    }

    /** Public clients need an actual HTTP failure status, unlike the legacy admin response convention. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(statusCode(ex)).body(R.fail(ex.getCode(), ex.getMessage()));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }
}
