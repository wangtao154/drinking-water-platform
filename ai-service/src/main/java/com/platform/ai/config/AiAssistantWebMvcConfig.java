package com.platform.ai.config;

import com.platform.ai.assistant.auth.AiAssistantJwtAuthInterceptor;
import com.platform.ai.assistant.auth.AiAssistantIdentityVerificationInterceptor;
import com.platform.ai.assistant.auth.AiAssistantConsentInterceptor;
import com.platform.ai.assistant.auth.AiAssistantPermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Keeps assistant authentication isolated from the existing RO prediction API.
 */
@Configuration
@RequiredArgsConstructor
public class AiAssistantWebMvcConfig implements WebMvcConfigurer {

    private final AiAssistantJwtAuthInterceptor jwtAuthInterceptor;
    private final AiAssistantIdentityVerificationInterceptor identityVerificationInterceptor;
    private final AiAssistantConsentInterceptor consentInterceptor;
    private final AiAssistantPermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .order(0)
                .addPathPatterns("/api/v1/ai/assistant/**");
        registry.addInterceptor(identityVerificationInterceptor)
                .order(1)
                .addPathPatterns("/api/v1/ai/assistant/**");
        registry.addInterceptor(consentInterceptor)
                .order(2)
                .addPathPatterns("/api/v1/ai/assistant/**");
        registry.addInterceptor(permissionInterceptor)
                .order(3)
                .addPathPatterns("/api/v1/ai/assistant/**");
    }
}
