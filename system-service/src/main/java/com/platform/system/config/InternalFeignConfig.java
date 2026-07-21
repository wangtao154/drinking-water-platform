package com.platform.system.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Value("${internal.service-token:drinking-water-internal-service-token-change-me}")
    private String internalServiceToken;

    @Bean
    public RequestInterceptor internalTokenRequestInterceptor() {
        return template -> template.header(INTERNAL_TOKEN_HEADER, internalServiceToken);
    }
}
