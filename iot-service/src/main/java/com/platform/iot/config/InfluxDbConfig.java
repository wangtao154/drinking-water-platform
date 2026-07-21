package com.platform.iot.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class InfluxDbConfig {

    @Value("${influxdb.url:http://dw-influxdb:8086}")
    private String url;

    @Value("${influxdb.token:drinking-water-super-token-please-change-in-production}")
    private String token;

    @Value("${influxdb.org:platform}")
    private String influxOrg;

    @Value("${influxdb.bucket:drinking_water}")
    private String influxBucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        try {
            InfluxDBClient client = InfluxDBClientFactory.create(url, token.toCharArray(), influxOrg, influxBucket);
            log.info("InfluxDB client created for url: {}", url);
            return client;
        } catch (Exception e) {
            log.error("Failed to create InfluxDB client: {}", e.getMessage(), e);
            throw e;
        }
    }
}
