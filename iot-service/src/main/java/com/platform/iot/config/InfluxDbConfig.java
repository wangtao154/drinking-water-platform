package com.platform.iot.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

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

    @Value("${influxdb.timeout-seconds:60}")
    private long timeoutSeconds;

    @Bean
    public InfluxDBClient influxDBClient() {
        try {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(timeoutSeconds, TimeUnit.SECONDS);

            InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                    .url(url)
                    .authenticateToken(token.toCharArray())
                    .org(influxOrg)
                    .bucket(influxBucket)
                    .okHttpClient(httpClient)
                    .build();

            InfluxDBClient client = InfluxDBClientFactory.create(options);
            log.info("InfluxDB client created for url: {}, timeout: {}s", url, timeoutSeconds);
            return client;
        } catch (Exception e) {
            log.error("Failed to create InfluxDB client: {}", e.getMessage(), e);
            throw e;
        }
    }
}
