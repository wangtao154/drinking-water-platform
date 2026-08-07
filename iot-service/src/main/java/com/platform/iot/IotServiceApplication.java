package com.platform.iot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.platform.iot", "com.platform.common"})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.platform.iot.mapper")
public class IotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotServiceApplication.class, args);
    }
}
