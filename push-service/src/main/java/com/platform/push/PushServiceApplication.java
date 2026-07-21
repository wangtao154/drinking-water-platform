package com.platform.push;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.platform.push", "com.platform.common"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.platform.push.mapper")
public class PushServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PushServiceApplication.class, args);
    }
}
