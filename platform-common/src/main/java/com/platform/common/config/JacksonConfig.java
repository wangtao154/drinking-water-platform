package com.platform.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Jackson JSON 序列化配置
 * - Long → String（防止前端精度丢失，雪花算法ID为19位，超过JS安全整数范围）
 * - LocalDateTime 格式化
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

        // Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

        // Long → String（防止雪花算法ID前端精度丢失）
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, new ToStringSerializer());
        longModule.addSerializer(Long.TYPE, new ToStringSerializer());

        builder.modules(javaTimeModule, longModule);
        builder.featuresToDisable(
                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        return builder;
    }

    /**
     * 公共线程池（用于异步审计日志等场景）
     */
    @Bean(name = "asyncExecutor")
    public ExecutorService asyncExecutor() {
        return Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "async-task");
            t.setDaemon(true);
            return t;
        });
    }
}
