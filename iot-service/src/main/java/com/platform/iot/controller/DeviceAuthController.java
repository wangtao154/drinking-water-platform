package com.platform.iot.controller;

import com.platform.iot.entity.Device;
import com.platform.iot.mapper.DeviceLookupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class DeviceAuthController {

    private final DeviceLookupMapper deviceLookupMapper;

    @PostMapping("/auth/device")
    public Map<String, String> authenticateDevice(@RequestBody Map<String, String> body) {
        String clientid = body.get("clientid");
        String username = body.get("username");
        String password = body.get("password");

        log.info("EMQX auth request for clientid: {}", clientid);

        if (clientid == null || clientid.isEmpty()) {
            return Map.of("result", "deny");
        }

        Device device = deviceLookupMapper.selectBySn(clientid);
        if (device == null) {
            log.warn("Device not found for SN: {}", clientid);
            return Map.of("result", "deny");
        }

        // Device must be in ACTIVATED state (ACTIVATED_ONLINE or ACTIVATED_OFFLINE)
        String lifecycle = device.getLifecycleStatus();
        if (lifecycle == null || !(lifecycle.startsWith("ACTIVATED"))) {
            log.warn("Device SN {} is not activated, lifecycle: {}", clientid, lifecycle);
            return Map.of("result", "deny");
        }

        log.info("Device SN {} authenticated successfully", clientid);
        return Map.of("result", "allow");
    }

    @GetMapping("/acl/device")
    public Map<String, String> authorizeDevice(@RequestParam String clientid,
                                                @RequestParam String topic,
                                                @RequestParam String action) {
        log.info("EMQX ACL request: clientid={}, topic={}, action={}", clientid, topic, action);

        // Device can only publish to its own topics
        if ("publish".equals(action)) {
            // Allowed topics: api/v2/upload/{sn}, api/v2/ack/{sn}
            if (topic.equals("api/v2/upload/" + clientid) ||
                topic.equals("api/v2/ack/" + clientid)) {
                return Map.of("result", "allow");
            }
            return Map.of("result", "deny");
        }

        // Device can subscribe to its own download and set topics
        if ("subscribe".equals(action)) {
            if (topic.equals("api/v2/download/" + clientid) ||
                topic.equals("api/v2/set/" + clientid)) {
                return Map.of("result", "allow");
            }
            return Map.of("result", "deny");
        }

        return Map.of("result", "deny");
    }
}
