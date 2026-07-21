package com.platform.iot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.iot.entity.SysConfig;
import com.platform.iot.mapper.SysConfigLookupMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MQTT 配置加载服务
 * 直接从共享数据库 sys_config 读取 mqtt.* 配置（与 system-service 共享同一 MySQL）
 * 支持运行时重新加载并触发重连
 */
@Slf4j
@Service
public class MqttConfigService {

    private final SysConfigLookupMapper sysConfigLookupMapper;

    @Value("${mqtt.default-broker:tcp://dw-emqx:1883}")
    private String defaultBroker;

    /** 当前生效的配置（启动时从数据库加载） */
    private volatile MqttConfigData currentConfig;

    public MqttConfigService(SysConfigLookupMapper sysConfigLookupMapper) {
        this.sysConfigLookupMapper = sysConfigLookupMapper;
    }

    @PostConstruct
    public void init() {
        try {
            loadFromDatabase();
        } catch (Exception e) {
            log.warn("[MQTT配置] 启动加载失败，使用本地默认值: {}", e.getMessage());
            currentConfig = defaultConfig();
        }
    }

    /**
     * 从 sys_config 表中加载 mqtt.* 配置
     */
    public synchronized void loadFromDatabase() {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SysConfig::getConfigKey, "mqtt.");
        List<SysConfig> all = sysConfigLookupMapper.selectList(wrapper);
        MqttConfigData cfg = defaultConfig();
        for (SysConfig c : all) {
            switch (c.getConfigKey()) {
                case "mqtt.name" -> cfg.setName(c.getConfigValue());
                case "mqtt.broker" -> cfg.setBroker(c.getConfigValue());
                case "mqtt.client_id" -> cfg.setClientId(c.getConfigValue());
                case "mqtt.username" -> cfg.setUsername(c.getConfigValue());
                case "mqtt.password" -> cfg.setPassword(c.getConfigValue());
                case "mqtt.keep_alive_interval" -> {
                    try { cfg.setKeepAliveInterval(Integer.parseInt(c.getConfigValue())); } catch (Exception ignored) {}
                }
                case "mqtt.clean_session" -> cfg.setCleanSession(Boolean.parseBoolean(c.getConfigValue()));
                case "mqtt.enabled" -> cfg.setEnabled(Boolean.parseBoolean(c.getConfigValue()));
                default -> {}
            }
        }
        currentConfig = cfg;
        log.info("[MQTT配置] 已从数据库加载: broker={}, enabled={}, keepAlive={}", cfg.getBroker(), cfg.getEnabled(), cfg.getKeepAliveInterval());
    }

    public MqttConfigData getCurrentConfig() {
        if (currentConfig == null) {
            currentConfig = defaultConfig();
        }
        return currentConfig;
    }

    private MqttConfigData defaultConfig() {
        MqttConfigData cfg = new MqttConfigData();
        cfg.setName("MQTT服务器");
        cfg.setBroker(defaultBroker);
        cfg.setClientId("iot-service");
        cfg.setUsername("admin");
        cfg.setPassword("public");
        cfg.setKeepAliveInterval(60);
        cfg.setCleanSession(false);
        cfg.setEnabled(true);
        return cfg;
    }

    /** 内部使用的扁平化配置（不含状态字段） */
    @Data
    public static class MqttConfigData {
        private String name;
        private String broker;
        private String clientId;
        private String username;
        private String password;
        private Integer keepAliveInterval = 60;
        private Boolean cleanSession = false;
        private Boolean enabled = true;
    }
}
