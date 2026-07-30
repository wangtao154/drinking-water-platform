package com.platform.water.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.UserContext;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.common.result.ResultCode;
import com.platform.water.config.WaterScanProperties;
import com.platform.water.config.WechatPayProperties;
import com.platform.water.dto.Q74PreviewDTO;
import com.platform.water.dto.WaterDispenseCreateDTO;
import com.platform.water.dto.WaterDispenseOrderPageQueryDTO;
import com.platform.water.entity.CustomerLookup;
import com.platform.water.entity.DeviceLookup;
import com.platform.water.entity.WaterDispenseEvent;
import com.platform.water.entity.WaterDispenseOrder;
import com.platform.water.feign.IotServiceClient;
import com.platform.water.mapper.CustomerLookupMapper;
import com.platform.water.mapper.DeviceLookupMapper;
import com.platform.water.mapper.WaterDispenseEventMapper;
import com.platform.water.mapper.WaterDispenseOrderMapper;
import com.platform.water.service.WaterDispenseService;
import com.platform.water.service.WechatPayClient;
import com.platform.water.vo.Q74ProtocolVO;
import com.platform.water.vo.WaterDispensePricePreviewVO;
import com.platform.water.vo.WaterDispenseOrderVO;
import com.platform.water.vo.WaterDispenseOrderStatsVO;
import com.platform.water.vo.WaterWechatPayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaterDispenseServiceImpl implements WaterDispenseService {

    private static final String PAY_PENDING = "PENDING";
    private static final String PAY_SUCCESS = "SUCCESS";
    private static final String DISPENSE_PENDING_PAY = "PENDING_PAY";
    private static final String DISPENSE_PAID = "PAID";
    private static final String DISPENSE_DISPATCHED = "DISPATCHED";
    private static final String COMMAND_PENDING = "PENDING";
    private static final String COMMAND_SENT = "SENT";

    private final WaterDispenseOrderMapper orderMapper;
    private final WaterDispenseEventMapper eventMapper;
    private final DeviceLookupMapper deviceLookupMapper;
    private final CustomerLookupMapper customerLookupMapper;
    private final IotServiceClient iotServiceClient;
    private final WechatPayClient wechatPayClient;
    private final WaterScanProperties properties;
    private final WechatPayProperties wechatPayProperties;

    @Value("${internal.service-token:drinking-water-internal-service-token-change-me}")
    private String internalServiceToken;

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
        order.setTargetMl(validateTargetMl(dto.getTargetMl()));
        order.setWaterType(validateWaterType(dto.getWaterType()));
        order.setPayAmount(calculatePayAmount(order.getTargetMl(), order.getWaterType()));
        order.setPayStatus(PAY_PENDING);
        order.setDispenseStatus(DISPENSE_PENDING_PAY);
        order.setCommandStatus(COMMAND_PENDING);
        order.setPaymentProvider("WECHAT");
        order.setMockPayment(false);
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);
        appendEvent(order.getOrderNo(), "ORDER_CREATED", "SUCCESS", "scan water order created", null);

        log.info("Water dispense order created: orderNo={}, sn={}, targetMl={}, waterType={}, amount={}",
                order.getOrderNo(), order.getSn(), order.getTargetMl(), order.getWaterType(), order.getPayAmount());
        return WaterDispenseOrderVO.fromEntity(order);
    }

    @Override
    public WaterDispensePricePreviewVO previewPrice(Long targetMl, Integer waterType) {
        Long normalizedTargetMl = validateTargetMl(targetMl);
        Integer normalizedWaterType = validateWaterType(waterType);
        return new WaterDispensePricePreviewVO(normalizedTargetMl, normalizedWaterType,
                calculatePayAmount(normalizedTargetMl, normalizedWaterType));
    }

    @Override
    @Transactional
    public WaterWechatPayVO prepareWechatPay(String orderNo) {
        WaterDispenseOrder order = selectOrder(orderNo);
        if (DISPENSE_DISPATCHED.equals(order.getDispenseStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "order has already dispatched");
        }
        if (!DISPENSE_PENDING_PAY.equals(order.getDispenseStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT,
                    "order status cannot create wechat pay: " + order.getDispenseStatus());
        }

        CurrentUser currentUser = UserContext.get();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "please login mini program first");
        }
        CustomerLookup customer = customerLookupMapper.selectById(currentUser.getUserId());
        if (customer == null || !StringUtils.hasText(customer.getOpenId())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "current user has no mini program openid");
        }

        String description = "Scan water " + order.getTargetMl() + "ml";
        String prepayId = wechatPayClient.createJsapiPrepay(order.getOrderNo(), description,
                order.getPayAmount(), customer.getOpenId());

        orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .set(WaterDispenseOrder::getPaymentProvider, "WECHAT")
                .set(WaterDispenseOrder::getMockPayment, false)
                .set(WaterDispenseOrder::getPrepayId, prepayId)
                .set(WaterDispenseOrder::getWxOpenId, customer.getOpenId())
                .set(WaterDispenseOrder::getPayStatus, PAY_PENDING));
        appendEvent(orderNo, "WECHAT_PREPAY_CREATED", "SUCCESS", "wechat prepay created", prepayId);

        WaterWechatPayVO vo = new WaterWechatPayVO();
        vo.setOrder(getByOrderNo(orderNo));
        vo.setPayParams(wechatPayClient.buildMiniProgramPayParams(prepayId));
        return vo;
    }

    @Override
    @Transactional
    public void handleWechatPayNotify(String timestamp, String nonce, String signature, String serial, String body) {
        JsonNode decrypted = wechatPayClient.decryptAndVerifyNotify(timestamp, nonce, signature, serial, body);
        String orderNo = decrypted.path("out_trade_no").asText("");
        String transactionId = decrypted.path("transaction_id").asText("");
        String tradeState = decrypted.path("trade_state").asText("");
        String summary = summarizeWechatPayNotify(decrypted);
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify missing out_trade_no");
        }

        WaterDispenseOrder order = selectOrder(orderNo);
        validateWechatPayNotify(order, decrypted);

        if (!PAY_SUCCESS.equals(tradeState)) {
            appendEvent(orderNo, "WECHAT_PAYMENT_NOTIFY", "FAIL", "wechat pay trade state: " + tradeState, summary);
            return;
        }

        if (DISPENSE_DISPATCHED.equals(order.getDispenseStatus()) && COMMAND_SENT.equals(order.getCommandStatus())) {
            appendEvent(orderNo, "WECHAT_PAYMENT_NOTIFY_DUPLICATE", "SUCCESS", "duplicate wechat pay notify", summary);
            return;
        }

        String payload = StringUtils.hasText(order.getQ74Payload())
                ? order.getQ74Payload()
                : buildQ74Payload("START", order.getTargetMl(), order.getWaterType());

        orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .set(WaterDispenseOrder::getPayStatus, PAY_SUCCESS)
                .set(WaterDispenseOrder::getDispenseStatus, DISPENSE_PAID)
                .set(WaterDispenseOrder::getTransactionId, transactionId)
                .set(WaterDispenseOrder::getPaymentProvider, "WECHAT")
                .set(WaterDispenseOrder::getMockPayment, false)
                .set(WaterDispenseOrder::getPaidAt, LocalDateTime.now())
                .set(WaterDispenseOrder::getQ74Payload, payload));
        appendEvent(orderNo, "WECHAT_PAYMENT_SUCCESS", "SUCCESS", "wechat payment success", summary);

        dispatchQ74AfterPay(orderNo, order.getSn(), order.getTargetMl(), order.getWaterType(), payload);
    }

    @Override
    public WaterDispenseOrderVO getByOrderNo(String orderNo) {
        return WaterDispenseOrderVO.fromEntity(selectOrder(orderNo));
    }

    @Override
    public PageResult<WaterDispenseOrderVO> pageOrders(WaterDispenseOrderPageQueryDTO query) {
        query.normalize();
        Page<WaterDispenseOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<WaterDispenseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getOrderNo()), WaterDispenseOrder::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getSn()), WaterDispenseOrder::getSn, query.getSn())
                .eq(StringUtils.hasText(query.getPayStatus()), WaterDispenseOrder::getPayStatus, query.getPayStatus())
                .eq(StringUtils.hasText(query.getDispenseStatus()), WaterDispenseOrder::getDispenseStatus, query.getDispenseStatus())
                .eq(StringUtils.hasText(query.getCommandStatus()), WaterDispenseOrder::getCommandStatus, query.getCommandStatus())
                .ge(query.getPaidStartTime() != null, WaterDispenseOrder::getPaidAt, query.getPaidStartTime())
                .le(query.getPaidEndTime() != null, WaterDispenseOrder::getPaidAt, query.getPaidEndTime());
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(WaterDispenseOrder::getOrderNo, keyword)
                    .or().like(WaterDispenseOrder::getSn, keyword)
                    .or().like(WaterDispenseOrder::getDeviceId, keyword)
                    .or().like(WaterDispenseOrder::getTransactionId, keyword));
        }
        wrapper.orderByDesc(WaterDispenseOrder::getCreatedAt);

        Page<WaterDispenseOrder> result = orderMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), (int) result.getSize(), result.getCurrent(),
                result.getRecords().stream().map(WaterDispenseOrderVO::fromEntity).toList());
    }

    @Override
    public WaterDispenseOrderStatsVO getOrderStats(WaterDispenseOrderPageQueryDTO query) {
        query.normalize();
        QueryWrapper<WaterDispenseOrder> wrapper = new QueryWrapper<WaterDispenseOrder>()
                .select(
                        "COUNT(*) AS total",
                        "SUM(CASE WHEN pay_status = 'SUCCESS' THEN 1 ELSE 0 END) AS paid",
                        "SUM(CASE WHEN pay_status = 'PENDING' THEN 1 ELSE 0 END) AS pending",
                        "SUM(CASE WHEN dispense_status = 'DISPATCHED' THEN 1 ELSE 0 END) AS dispatched",
                        "SUM(CASE WHEN command_status = 'SENT' THEN 1 ELSE 0 END) AS sent",
                        "COALESCE(SUM(CASE WHEN pay_status = 'SUCCESS' THEN pay_amount ELSE 0 END), 0) AS totalAmount");
        wrapper.eq(StringUtils.hasText(query.getOrderNo()), "order_no", query.getOrderNo())
                .eq(StringUtils.hasText(query.getSn()), "sn", query.getSn())
                .eq(StringUtils.hasText(query.getPayStatus()), "pay_status", query.getPayStatus())
                .eq(StringUtils.hasText(query.getDispenseStatus()), "dispense_status", query.getDispenseStatus())
                .eq(StringUtils.hasText(query.getCommandStatus()), "command_status", query.getCommandStatus())
                .ge(query.getPaidStartTime() != null, "paid_at", query.getPaidStartTime())
                .le(query.getPaidEndTime() != null, "paid_at", query.getPaidEndTime());
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like("order_no", keyword)
                    .or().like("sn", keyword)
                    .or().like("device_id", keyword)
                    .or().like("transaction_id", keyword));
        }
        List<Map<String, Object>> rows = orderMapper.selectMaps(wrapper);
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        WaterDispenseOrderStatsVO vo = new WaterDispenseOrderStatsVO();
        vo.setTotal(toLong(row.get("total")));
        vo.setPaid(toLong(row.get("paid")));
        vo.setPending(toLong(row.get("pending")));
        vo.setDispatched(toLong(row.get("dispatched")));
        vo.setSent(toLong(row.get("sent")));
        vo.setTotalAmount(toLong(row.get("totalAmount")));
        return vo;
    }

    @Override
    public Q74ProtocolVO previewQ74(Q74PreviewDTO dto) {
        return new Q74ProtocolVO(properties.getCommandPoint(),
                buildQ74Payload(dto.getAction(), dto.getTargetMl(), dto.getWaterType()));
    }

    private void dispatchQ74AfterPay(String orderNo, String sn, Long targetMl, Integer waterType, String payload) {
        String commandPayload = StringUtils.hasText(payload) ? payload : buildQ74Payload("START", targetMl, waterType);
        sendQ74(sn, commandPayload);

        orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .set(WaterDispenseOrder::getDispenseStatus, DISPENSE_DISPATCHED)
                .set(WaterDispenseOrder::getCommandStatus, COMMAND_SENT)
                .set(WaterDispenseOrder::getQ74Payload, commandPayload)
                .set(WaterDispenseOrder::getCommandSentAt, LocalDateTime.now()));
        appendEvent(orderNo, "Q74_COMMAND_SENT", "SUCCESS", "Q74 command sent", commandPayload);
    }

    private void sendQ74(String sn, String payload) {
        R<Object> response = iotServiceClient.sendSetCommand(sn, Map.of(
                "pointID", properties.getCommandPoint(),
                "value", payload
        ), internalServiceToken);
        if (response == null || response.getCode() == null || response.getCode() != 200) {
            String message = response != null ? response.getMessage() : "iot-service no response";
            throw new BusinessException(ResultCode.MQTT_COMMAND_FAILED, "Q74 dispatch failed: " + message);
        }
    }

    private String buildQ74Payload(String action, Long targetMl, Integer waterType) {
        String normalizedAction = action.trim().toUpperCase(Locale.ROOT);
        if (!normalizedAction.matches("START|STOP|CANCEL")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "unsupported water scan action: " + action);
        }
        Long normalizedTargetMl = validateTargetMl(targetMl);
        Integer normalizedWaterType = validateWaterType(waterType);

        long ts = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        String unsignedPayload = String.join("_",
                properties.getProtocolVersion(),
                normalizedAction,
                String.valueOf(normalizedTargetMl),
                String.valueOf(normalizedWaterType),
                String.valueOf(ts),
                nonce
        );
        return unsignedPayload + "_" + sign(unsignedPayload);
    }

    private Long validateTargetMl(Long targetMl) {
        if (targetMl == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "请输入取水量");
        }
        Long minTargetMl = properties.getMinTargetMl() != null ? properties.getMinTargetMl() : 1L;
        Long maxTargetMl = properties.getMaxTargetMl() != null ? properties.getMaxTargetMl() : 10000L;
        if (targetMl < minTargetMl || targetMl > maxTargetMl) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                    "取水量必须在 " + minTargetMl + "ml 到 " + maxTargetMl + "ml 之间");
        }
        return targetMl;
    }

    private Integer validateWaterType(Integer waterType) {
        if (waterType == null) {
            return 0;
        }
        if (waterType != 0 && waterType != 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "出水类型只能选择冷水或热水");
        }
        return waterType;
    }

    private Long calculatePayAmount(Long targetMl, Integer waterType) {
        validateWaterType(waterType);
        long minPayAmount = properties.getMinPayAmount() != null ? properties.getMinPayAmount() : 1L;
        long unitPriceCentsPerLiter = properties.getUnitPriceCentsPerLiter() != null
                ? properties.getUnitPriceCentsPerLiter()
                : 0L;
        if (unitPriceCentsPerLiter <= 0) {
            return minPayAmount;
        }
        long amount = (targetMl * unitPriceCentsPerLiter + 999L) / 1000L;
        return Math.max(minPayAmount, amount);
    }

    private void validateWechatPayNotify(WaterDispenseOrder order, JsonNode decrypted) {
        String appId = decrypted.path("appid").asText("");
        String mchId = decrypted.path("mchid").asText("");
        String transactionId = decrypted.path("transaction_id").asText("");
        String payerOpenId = decrypted.path("payer").path("openid").asText("");
        JsonNode amountNode = decrypted.path("amount");
        long total = amountNode.path("total").asLong(-1);
        String currency = amountNode.path("currency").asText("CNY");

        if (!StringUtils.hasText(order.getPrepayId()) || !"WECHAT".equals(order.getPaymentProvider())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "order has not created wechat prepay");
        }
        if (!Objects.equals(wechatPayProperties.getAppId(), appId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify appid mismatch");
        }
        if (!Objects.equals(wechatPayProperties.getMchId(), mchId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify mchid mismatch");
        }
        if (!Objects.equals(total, order.getPayAmount())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify amount mismatch");
        }
        if (StringUtils.hasText(currency) && !"CNY".equals(currency)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify currency mismatch");
        }
        if (!StringUtils.hasText(order.getWxOpenId()) || !Objects.equals(order.getWxOpenId(), payerOpenId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify payer mismatch");
        }
        if (!StringUtils.hasText(transactionId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat pay notify transaction id missing");
        }
        WaterDispenseOrder sameTransactionOrder = orderMapper.selectOne(new LambdaQueryWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getTransactionId, transactionId)
                .ne(WaterDispenseOrder::getOrderNo, order.getOrderNo())
                .last("LIMIT 1"));
        if (sameTransactionOrder != null) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "wechat pay transaction already exists");
        }
    }

    private String summarizeWechatPayNotify(JsonNode node) {
        String transactionId = maskTail(node.path("transaction_id").asText(""));
        String payerOpenId = maskTail(node.path("payer").path("openid").asText(""));
        long amount = node.path("amount").path("total").asLong(-1);
        return "out_trade_no=" + node.path("out_trade_no").asText("")
                + ", transaction_id=" + transactionId
                + ", trade_state=" + node.path("trade_state").asText("")
                + ", amount_total=" + amount
                + ", payer_openid=" + payerOpenId;
    }

    private String maskTail(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int visible = Math.min(6, value.length());
        return "***" + value.substring(value.length() - visible);
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
            throw new BusinessException(ResultCode.NOT_FOUND, "scan water order not found: " + orderNo);
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
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "device not found");
        }
        if (device.getSn() == null || device.getSn().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "device sn is empty, cannot dispatch Q74");
        }
        return device;
    }

    private String generateOrderNo() {
        return "WD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
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
