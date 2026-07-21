package com.platform.system.feign;

import com.platform.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * iot-service Feign 客户端
 * 用于通知 iot-service 重连 MQTT、查询运行状态
 */
@FeignClient(name = "iot-service", fallback = IotServiceClient.IotServiceFallback.class)
public interface IotServiceClient {

    /** 触发 iot-service 重新加载 MQTT 配置并重连 */
    @PostMapping("/api/v1/iot/mqtt/reload")
    R<Void> reloadMqtt();

    /** 查询 iot-service MQTT 运行状态 */
    @GetMapping("/api/v1/iot/mqtt/status")
    R<Map<String, Object>> mqttStatus();

    @org.springframework.stereotype.Component
    class IotServiceFallback implements IotServiceClient {
        @Override
        public R<Void> reloadMqtt() {
            return R.fail(503, "iot-service 不可用");
        }

        @Override
        public R<Map<String, Object>> mqttStatus() {
            return R.ok(java.util.Map.of("running", false, "connected", false, "error", "iot-service 不可用"));
        }
    }
}
