package com.platform.ai;

import com.platform.ai.config.AdminAssistantProperties;
import com.platform.ai.config.AdminAssistantToolProperties;
import com.platform.ai.config.QwenProperties;
import com.platform.ai.config.RoPredictionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        QwenProperties.class,
        RoPredictionProperties.class,
        AdminAssistantProperties.class,
        AdminAssistantToolProperties.class
})
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
