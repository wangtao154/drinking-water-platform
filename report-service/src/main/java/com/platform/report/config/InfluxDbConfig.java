package com.platform.report.config;

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

    @Value("${influxdb.timeout-seconds:180}")
    private long timeoutSeconds;

    @Bean
    public InfluxDBClient influxDBClient() {
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
        log.info("[报表] InfluxDB 客户端已创建: {}, timeout: {}s", url, timeoutSeconds);
        return client;
    }
}
