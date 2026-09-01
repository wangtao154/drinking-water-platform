package com.platform.ai;

import com.platform.ai.config.AdminAssistantProperties;
import com.platform.ai.config.AdminAssistantAuditProperties;
import com.platform.ai.config.AdminAssistantToolProperties;
import com.platform.ai.config.QwenProperties;
import com.platform.ai.config.RoPredictionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        QwenProperties.class,
        RoPredictionProperties.class,
        AdminAssistantProperties.class,
        AdminAssistantAuditProperties.class,
        AdminAssistantToolProperties.class
})
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
