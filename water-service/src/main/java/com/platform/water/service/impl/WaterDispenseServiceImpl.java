package com.platform.water.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.UserContext;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.R;
import com.platform.common.result.ResultCode;
import com.platform.water.config.WaterScanProperties;
import com.platform.water.dto.Q74PreviewDTO;
import com.platform.water.dto.WaterDispenseCreateDTO;
import com.platform.water.entity.DeviceLookup;
import com.platform.water.entity.WaterDispenseEvent;
import com.platform.water.entity.WaterDispenseOrder;
import com.platform.water.feign.IotServiceClient;
import com.platform.water.mapper.DeviceLookupMapper;
import com.platform.water.mapper.WaterDispenseEventMapper;
import com.platform.water.mapper.WaterDispenseOrderMapper;
import com.platform.water.service.WaterDispenseService;
import com.platform.water.vo.Q74ProtocolVO;
import com.platform.water.vo.WaterDispenseOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaterDispenseServiceImpl implements WaterDispenseService {

    private static final String DISPENSE_PENDING_PAY = "PENDING_PAY";
    private static final String DISPENSE_PAID = "PAID";
    private static final String DISPENSE_DISPATCHED = "DISPATCHED";
    private static final String COMMAND_PENDING = "PENDING";
    private static final String COMMAND_SENT = "SENT";

    private final WaterDispenseOrderMapper orderMapper;
    private final WaterDispenseEventMapper eventMapper;
    private final DeviceLookupMapper deviceLookupMapper;
    private final IotServiceClient iotServiceClient;
    private final WaterScanProperties properties;

    @Override
    @Transactional
    public WaterDispenseOrderVO createOrder(WaterDispenseCreateDTO dto) {
        DeviceLookup device = resolveDevice(dto.getDeviceId(), dto.getSn());

        CurrentUser currentUser = UserContext.get();
        WaterDispenseOrder order = new WaterDispenseOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(currentUser != null ? currentUser.getUserId() : null);
        order.setDeviceId(device.getDeviceId());
        order.setSn(device.getSn());
        order.setTargetMl(dto.getTargetMl());
        order.setPayAmount(dto.getPayAmount() != null ? dto.getPayAmount() : properties.getMockDefaultAmount());
        order.setPayStatus("PENDING");
        order.setDispenseStatus(DISPENSE_PENDING_PAY);
        order.setCommandStatus(COMMAND_PENDING);
        order.setMockPayment(true);
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);
        appendEvent(order.getOrderNo(), "ORDER_CREATED", "SUCCESS", "扫码取水订单已创建", null);

        log.info("Water dispense order created: orderNo={}, sn={}, targetMl={}, amount={}",
                order.getOrderNo(), order.getSn(), order.getTargetMl(), order.getPayAmount());
        return WaterDispenseOrderVO.fromEntity(order);
    }

    @Override
    @Transactional
    public WaterDispenseOrderVO mockPayAndDispatch(String orderNo) {
        WaterDispenseOrder order = selectOrder(orderNo);
        if (DISPENSE_DISPATCHED.equals(order.getDispenseStatus())) {
            return WaterDispenseOrderVO.fromEntity(order);
        }
        if (!DISPENSE_PENDING_PAY.equals(order.getDispenseStatus()) && !DISPENSE_PAID.equals(order.getDispenseStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "当前订单状态不允许模拟支付: " + order.getDispenseStatus());
        }

        String transactionId = "MOCK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        String payload = buildQ74Payload("START", order.getTargetMl());
        LocalDateTime now = LocalDateTime.now();

        orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .set(WaterDispenseOrder::getPayStatus, "SUCCESS")
                .set(WaterDispenseOrder::getDispenseStatus, DISPENSE_PAID)
                .set(WaterDispenseOrder::getTransactionId, transactionId)
                .set(WaterDispenseOrder::getPaidAt, now)
                .set(WaterDispenseOrder::getQ74Payload, payload));
        appendEvent(orderNo, "MOCK_PAYMENT_SUCCESS", "SUCCESS", "模拟支付成功", transactionId);

        sendQ74(order.getSn(), payload);

        orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .set(WaterDispenseOrder::getDispenseStatus, DISPENSE_DISPATCHED)
                .set(WaterDispenseOrder::getCommandStatus, COMMAND_SENT)
                .set(WaterDispenseOrder::getCommandSentAt, LocalDateTime.now()));
        appendEvent(orderNo, "Q74_COMMAND_SENT", "SUCCESS", "扫码出水信息已下发", payload);

        return getByOrderNo(orderNo);
    }

    @Override
    public WaterDispenseOrderVO getByOrderNo(String orderNo) {
        return WaterDispenseOrderVO.fromEntity(selectOrder(orderNo));
    }

    @Override
    public Q74ProtocolVO previewQ74(Q74PreviewDTO dto) {
        return new Q74ProtocolVO(properties.getCommandPoint(), buildQ74Payload(dto.getAction(), dto.getTargetMl()));
    }

    private void sendQ74(String sn, String payload) {
        R<Object> response = iotServiceClient.sendSetCommand(sn, Map.of(
                "pointID", properties.getCommandPoint(),
                "value", payload
        ));
        if (response == null || response.getCode() == null || response.getCode() != 200) {
            String message = response != null ? response.getMessage() : "iot-service no response";
            throw new BusinessException(ResultCode.MQTT_COMMAND_FAILED, "Q74下发失败: " + message);
        }
    }

    private String buildQ74Payload(String action, Long targetMl) {
        String normalizedAction = action.trim().toUpperCase(Locale.ROOT);
        if (!normalizedAction.matches("START|STOP|CANCEL")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的扫码出水动作: " + action);
        }

        long ts = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        String unsignedPayload = String.join("_",
                properties.getProtocolVersion(),
                normalizedAction,
                String.valueOf(targetMl),
                String.valueOf(ts),
                nonce
        );
        return unsignedPayload + "_" + sign(unsignedPayload);
    }

    private String sign(String unsignedPayload) {
        CRC32 crc32 = new CRC32();
        crc32.update((unsignedPayload + "_" + properties.getSignSecret()).getBytes(StandardCharsets.UTF_8));
        return String.format("%08X", crc32.getValue());
    }

    private WaterDispenseOrder selectOrder(String orderNo) {
        WaterDispenseOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<WaterDispenseOrder>().eq(WaterDispenseOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "扫码取水订单不存在: " + orderNo);
        }
        return order;
    }

    private DeviceLookup resolveDevice(String deviceId, String sn) {
        DeviceLookup device = null;
        if (StringUtils.hasText(deviceId)) {
            device = deviceLookupMapper.selectByDeviceId(deviceId);
        }
        if (device == null && StringUtils.hasText(sn)) {
            device = deviceLookupMapper.selectBySn(sn);
        }
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在");
        }
        if (device.getSn() == null || device.getSn().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "设备SN为空，无法下发Q74");
        }
        return device;
    }

    private String generateOrderNo() {
        return "WD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private void appendEvent(String orderNo, String eventType, String status, String message, String rawData) {
        WaterDispenseEvent event = new WaterDispenseEvent();
        event.setOrderNo(orderNo);
        event.setEventType(eventType);
        event.setEventStatus(status);
        event.setEventMessage(message);
        event.setRawData(rawData);
        eventMapper.insert(event);
    }
}
