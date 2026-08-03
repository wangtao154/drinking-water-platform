package com.platform.water.feign;

import com.platform.common.result.R;
import com.platform.water.vo.IotCommandAckResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "iot-service")
public interface IotServiceClient {

    @PostMapping("/api/v1/iot/internal/devices/{sn}/set")
    R<Object> sendSetCommand(
            @PathVariable("sn") String sn,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-Internal-Token") String internalToken);

    @PostMapping("/api/v1/iot/internal/devices/{sn}/set-with-ack")
    R<IotCommandAckResultVO> sendSetCommandWithAck(
            @PathVariable("sn") String sn,
            @RequestBody Map<String, String> body,
            @RequestParam("maxAttempts") Integer maxAttempts,
            @RequestParam("ackTimeoutMs") Long ackTimeoutMs,
            @RequestHeader("X-Internal-Token") String internalToken);
}
