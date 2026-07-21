package com.platform.iot.config;

import com.platform.iot.mqtt.MqttMessageHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * MQTT 客户端管理
 * 支持运行时重新加载配置并重连
 */
@Slf4j
@Configuration
public class MqttConfig {

    private final MqttMessageHandler messageHandler;
    private final MqttConfigService mqttConfigService;

    /** 当前连接是否已建立（运行状态） */
    private volatile boolean running = false;
    /** 当前链路是否已连接（链路状态） */
    private volatile boolean connected = false;
    /** 最后一次错误信息 */
    private volatile String lastError;
    /** MQTT 客户端 */
    private volatile MqttClient mqttClient;

    public MqttConfig(MqttMessageHandler messageHandler, MqttConfigService mqttConfigService) {
        this.messageHandler = messageHandler;
        this.mqttConfigService = mqttConfigService;
    }

    /**
     * 暴露 MqttClient 供其他服务注入使用（如 CommandService 用于下发指令）
     * 注意：mqttClient 是 volatile 字段，连接时由 connectWithCurrentConfig 初始化
     */
    @Bean
    public MqttClient mqttClient() {
        return mqttClient;
    }

    @PostConstruct
    public void init() {
        // 启动时同步连接，确保 MqttClient bean 立即可用
        connectWithCurrentConfig();
    }

    /**
     * 根据当前配置连接 broker
     */
    public synchronized void connectWithCurrentConfig() {
        var cfg = mqttConfigService.getCurrentConfig();
        if (Boolean.FALSE.equals(cfg.getEnabled())) {
            log.info("[MQTT] 当前配置为禁用状态，跳过连接");
            running = false;
            connected = false;
            return;
        }
        try {
            // 先断开旧连接
            disconnect();
            MqttConfigService.MqttConfigData c = mqttConfigService.getCurrentConfig();
            log.info("[MQTT] 准备连接 broker={}, clientId={}, username={}", c.getBroker(), c.getClientId(), c.getUsername());

            mqttClient = new MqttClient(c.getBroker(), c.getClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(Boolean.TRUE.equals(c.getCleanSession()));
            options.setKeepAliveInterval(c.getKeepAliveInterval() == null ? 60 : c.getKeepAliveInterval());
            options.setConnectionTimeout(10);
            if (c.getUsername() != null && !c.getUsername().isEmpty()) {
                options.setUserName(c.getUsername());
            }
            if (c.getPassword() != null && !c.getPassword().isEmpty()) {
                options.setPassword(c.getPassword().toCharArray());
            }

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("[MQTT] 连接断开: {}", cause.getMessage());
                    connected = false;
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        String payload = new String(message.getPayload());
                        log.debug("[MQTT] 收到消息 topic={}", topic);

                        if (topic.startsWith("api/v2/data/")) {
                            String sn = topic.substring("api/v2/data/".length());
                            messageHandler.handleTelemetry(sn, payload);
                        } else if (topic.startsWith("api/v2/lwt/")) {
                            String sn = topic.substring("api/v2/lwt/".length());
                            messageHandler.handleLwt(sn, payload);
                        } else if (topic.startsWith("api/v2/ack/")) {
                            String sn = topic.substring("api/v2/ack/".length());
                            messageHandler.handleAck(sn, payload);
                        } else {
                            log.warn("[MQTT] 未知主题: {}", topic);
                        }
                    } catch (Exception e) {
                        log.error("[MQTT] 处理消息失败 topic={}: {}", topic, e.getMessage(), e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.debug("[MQTT] 消息投递完成: {}", token.getMessageId());
                }
            });

            mqttClient.connect(options);
            connected = mqttClient.isConnected();
            running = true;
            lastError = null;
            log.info("[MQTT] 已连接 broker={}", c.getBroker());

            // 订阅主题
            mqttClient.subscribe("api/v2/data/#", 1);
            mqttClient.subscribe("api/v2/lwt/#", 1);
            mqttClient.subscribe("api/v2/ack/#", 1);
            log.info("[MQTT] 订阅完成: data/#, lwt/#, ack/#");
        } catch (Exception e) {
            log.error("[MQTT] 连接失败: {}", e.getMessage(), e);
            running = true;  // 服务在运行，只是连接失败
            connected = false;
            lastError = e.getMessage();
        }
    }

    /**
     * 重新加载配置并重连（外部调用入口）
     */
    public synchronized void reload() {
        log.info("[MQTT] 收到重载请求");
        try {
            mqttConfigService.loadFromDatabase();
        } catch (Exception e) {
            log.warn("[MQTT] 拉取新配置失败，保持当前配置: {}", e.getMessage());
        }
        connectWithCurrentConfig();
    }

    private void disconnect() {
        if (mqttClient != null) {
            try {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                mqttClient.close();
            } catch (Exception e) {
                log.warn("[MQTT] 关闭旧连接异常: {}", e.getMessage());
            }
            mqttClient = null;
        }
    }

    @PreDestroy
    public void destroy() {
        disconnect();
        running = false;
        connected = false;
        log.info("[MQTT] 客户端已销毁");
    }

    public boolean isRunning() { return running; }
    public boolean isConnected() { return connected; }
    public String getLastError() { return lastError; }
}
