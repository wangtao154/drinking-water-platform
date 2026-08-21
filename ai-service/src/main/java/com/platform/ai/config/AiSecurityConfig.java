package com.platform.ai.config;

import com.platform.common.auth.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI service does not scan the platform-common package by default. Register
 * the shared JWT utility explicitly so local, trusted calls can validate a
 * Bearer token when gateway identity headers are unavailable.
 */
@Configuration
public class AiSecurityConfig {

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }
}
