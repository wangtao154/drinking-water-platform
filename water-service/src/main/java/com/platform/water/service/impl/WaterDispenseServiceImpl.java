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
import com.platform.water.vo.IotCommandAckResultVO;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    private static final String PAY_REFUND = "REFUND";
    private static final String DISPENSE_PENDING_PAY = "PENDING_PAY";
    private static final String DISPENSE_PAID = "PAID";
    private static final String DISPENSE_DISPATCHED = "DISPATCHED";
    private static final String DISPENSE_FAILED = "FAILED";
    private static final String DISPENSE_REFUNDED = "REFUNDED";
    private static final String COMMAND_PENDING = "PENDING";
    private static final String COMMAND_SENT = "SENT";
    private static final String COMMAND_ACK = "ACK";
    private static final String COMMAND_FAILED = "FAILED";
    private static final String REFUND_NONE = "NONE";
    private static final String REFUND_PROCESSING = "PROCESSING";
    private static final String REFUND_SUCCESS = "SUCCESS";
    private static final String REFUND_FAILED = "FAILED";

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
        order.setRefundStatus(REFUND_NONE);
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
        ensureDeviceOnline(resolveDevice(order.getDeviceId(), order.getSn()));

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

        if (PAY_SUCCESS.equals(order.getPayStatus()) && StringUtils.hasText(order.getTransactionId())) {
            appendEvent(orderNo, "WECHAT_PAYMENT_NOTIFY_DUPLICATE", "SUCCESS", "duplicate wechat pay notify", summary);
            return;
        }

        String payload = StringUtils.hasText(order.getQ74Payload())
                ? order.getQ74Payload()
                : buildQ74Payload("START", order.getTargetMl(), order.getWaterType());

        int updatedRows = orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .ne(WaterDispenseOrder::getPayStatus, PAY_SUCCESS)
                .set(WaterDispenseOrder::getPayStatus, PAY_SUCCESS)
                .set(WaterDispenseOrder::getDispenseStatus, DISPENSE_PAID)
                .set(WaterDispenseOrder::getTransactionId, transactionId)
                .set(WaterDispenseOrder::getPaymentProvider, "WECHAT")
                .set(WaterDispenseOrder::getMockPayment, false)
                .set(WaterDispenseOrder::getPaidAt, LocalDateTime.now())
                .set(WaterDispenseOrder::getQ74Payload, payload));
        if (updatedRows <= 0) {
            appendEvent(orderNo, "WECHAT_PAYMENT_NOTIFY_DUPLICATE", "SUCCESS",
                    "duplicate wechat pay notify skipped dispatch", summary);
            return;
        }
        appendEvent(orderNo, "WECHAT_PAYMENT_SUCCESS", "SUCCESS", "wechat payment success", summary);

        dispatchQ74AfterPay(orderNo, order.getSn(), order.getTargetMl(), order.getWaterType(), payload);
    }

    @Override
    @Transactional
    public void handleWechatRefundNotify(String timestamp, String nonce, String signature, String serial, String body) {
        JsonNode decrypted = wechatPayClient.decryptAndVerifyNotify(timestamp, nonce, signature, serial, body);
        String orderNo = decrypted.path("out_trade_no").asText("");
        String refundNo = decrypted.path("out_refund_no").asText("");
        String refundStatus = decrypted.path("refund_status").asText("");
        String refundId = decrypted.path("refund_id").asText("");
        long refundAmount = decrypted.path("amount").path("refund").asLong(-1);
        String summary = summarizeWechatRefundNotify(decrypted);

        if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(refundNo)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat refund notify missing order or refund no");
        }
        WaterDispenseOrder order = selectOrder(orderNo);
        if (!Objects.equals(order.getRefundNo(), refundNo)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat refund notify refund no mismatch");
        }
        if (refundAmount >= 0 && order.getRefundAmount() != null && !Objects.equals(order.getRefundAmount(), refundAmount)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat refund notify amount mismatch");
        }
        if (REFUND_SUCCESS.equals(order.getRefundStatus())) {
            appendEvent(orderNo, "WECHAT_REFUND_NOTIFY_DUPLICATE", "SUCCESS", "duplicate refund notify", summary);
            return;
        }

        if ("SUCCESS".equals(refundStatus)) {
            LocalDateTime successTime = parseWechatTime(decrypted.path("success_time").asText(""));
            markRefundSuccess(orderNo, refundNo, refundId, successTime != null ? successTime : LocalDateTime.now(), summary);
            return;
        }
        if ("PROCESSING".equals(refundStatus)) {
            orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                    .eq(WaterDispenseOrder::getOrderNo, orderNo)
                    .eq(WaterDispenseOrder::getRefundNo, refundNo)
                    .set(WaterDispenseOrder::getRefundStatus, REFUND_PROCESSING)
                    .set(WaterDispenseOrder::getWechatRefundId, refundId)
                    .set(WaterDispenseOrder::getRefundErrorMsg, null));
            appendEvent(orderNo, "WECHAT_REFUND_PROCESSING", "SUCCESS", "wechat refund processing", summary);
            return;
        }

        markRefundFailed(orderNo, refundNo, refundId,
                StringUtils.hasText(refundStatus) ? "wechat refund status: " + refundStatus : "wechat refund failed",
                summary);
    }

    @Override
    public void autoRefundRecentAbnormalOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<WaterDispenseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getPayStatus, PAY_SUCCESS)
                .eq(WaterDispenseOrder::getDispenseStatus, DISPENSE_FAILED)
                .eq(WaterDispenseOrder::getCommandStatus, COMMAND_FAILED)
                .eq(WaterDispenseOrder::getPaymentProvider, "WECHAT")
                .eq(WaterDispenseOrder::getMockPayment, false)
                .ge(WaterDispenseOrder::getPaidAt, now.minusMinutes(2))
                .le(WaterDispenseOrder::getPaidAt, now.minusSeconds(30))
                .and(w -> w.isNull(WaterDispenseOrder::getRefundStatus)
                        .or().in(WaterDispenseOrder::getRefundStatus, REFUND_NONE, REFUND_FAILED))
                .orderByAsc(WaterDispenseOrder::getPaidAt)
                .last("LIMIT 50"));
        for (WaterDispenseOrder order : orders) {
            try {
                requestRefund(order, "出水异常自动退款", true);
            } catch (Exception e) {
                log.warn("Scan water auto refund failed: orderNo={}, message={}", order.getOrderNo(), e.getMessage());
            }
        }
    }

    @Override
    public void syncProcessingRefunds() {
        List<WaterDispenseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getRefundStatus, REFUND_PROCESSING)
                .eq(WaterDispenseOrder::getPaymentProvider, "WECHAT")
                .eq(WaterDispenseOrder::getMockPayment, false)
                .isNotNull(WaterDispenseOrder::getRefundNo)
                .orderByAsc(WaterDispenseOrder::getRefundRequestedAt)
                .last("LIMIT 100"));
        for (WaterDispenseOrder order : orders) {
            try {
                syncRefundStatus(order, wechatPayClient.queryRefund(order.getRefundNo()));
            } catch (Exception e) {
                log.warn("Scan water refund status sync failed: orderNo={}, message={}",
                        order.getOrderNo(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public WaterDispenseOrderVO retryRefund(String orderNo) {
        WaterDispenseOrder order = selectOrder(orderNo);
        return requestRefund(order, "出水异常手动重试退款", false);
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
                .eq(StringUtils.hasText(query.getRefundStatus()), WaterDispenseOrder::getRefundStatus, query.getRefundStatus())
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
                        "SUM(CASE WHEN command_status IN ('SENT', 'ACK') THEN 1 ELSE 0 END) AS sent",
                        "SUM(CASE WHEN refund_status = 'SUCCESS' THEN 1 ELSE 0 END) AS refunded",
                        "SUM(CASE WHEN refund_status = 'PROCESSING' THEN 1 ELSE 0 END) AS refundProcessing",
                        "COALESCE(SUM(CASE WHEN pay_status = 'SUCCESS' THEN pay_amount ELSE 0 END), 0) AS totalAmount",
                        "COALESCE(SUM(CASE WHEN refund_status = 'SUCCESS' THEN refund_amount ELSE 0 END), 0) AS refundAmount",
                        "COALESCE(SUM(CASE WHEN pay_status = 'SUCCESS' THEN target_ml ELSE 0 END), 0) AS totalDispenseMl");
        wrapper.eq(StringUtils.hasText(query.getOrderNo()), "order_no", query.getOrderNo())
                .eq(StringUtils.hasText(query.getSn()), "sn", query.getSn())
                .eq(StringUtils.hasText(query.getPayStatus()), "pay_status", query.getPayStatus())
                .eq(StringUtils.hasText(query.getDispenseStatus()), "dispense_status", query.getDispenseStatus())
                .eq(StringUtils.hasText(query.getCommandStatus()), "command_status", query.getCommandStatus())
                .eq(StringUtils.hasText(query.getRefundStatus()), "refund_status", query.getRefundStatus())
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
        vo.setRefunded(toLong(row.get("refunded")));
        vo.setRefundProcessing(toLong(row.get("refundProcessing")));
        vo.setTotalAmount(toLong(row.get("totalAmount")));
        vo.setRefundAmount(toLong(row.get("refundAmount")));
        vo.setTotalDispenseMl(toLong(row.get("totalDispenseMl")));
        return vo;
    }

    @Override
    public Q74ProtocolVO previewQ74(Q74PreviewDTO dto) {
        return new Q74ProtocolVO(properties.getCommandPoint(),
                buildQ74Payload(dto.getAction(), dto.getTargetMl(), dto.getWaterType()));
    }

    private void dispatchQ74AfterPay(String orderNo, String sn, Long targetMl, Integer waterType, String payload) {
        String commandPayload = StringUtils.hasText(payload) ? payload : buildQ74Payload("START", targetMl, waterType);
        LocalDateTime sentAt = LocalDateTime.now();

        try {
            IotCommandAckResultVO ackResult = sendQ74WithAck(sn, commandPayload);
            boolean acknowledged = ackResult != null && Boolean.TRUE.equals(ackResult.getAcknowledged());
            if (acknowledged) {
                orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                        .eq(WaterDispenseOrder::getOrderNo, orderNo)
                        .set(WaterDispenseOrder::getDispenseStatus, DISPENSE_DISPATCHED)
                        .set(WaterDispenseOrder::getCommandStatus, COMMAND_ACK)
                        .set(WaterDispenseOrder::getQ74Payload, commandPayload)
                        .set(WaterDispenseOrder::getCommandSentAt, sentAt));
                appendEvent(orderNo, "Q74_COMMAND_ACK", "SUCCESS",
                        "Q74 command acknowledged, attempts=" + ackResult.getAttempts()
                                + ", messageId=" + ackResult.getLastMessageId(),
                        commandPayload);
                return;
            }

            String reason = ackResult != null && StringUtils.hasText(ackResult.getErrorMessage())
                    ? ackResult.getErrorMessage()
                    : "未收到设备ACK";
            markQ74DispatchFailed(orderNo, commandPayload, sentAt, reason);
        } catch (Exception e) {
            log.warn("Q74 dispatch failed after payment: orderNo={}, sn={}, message={}", orderNo, sn, e.getMessage());
            markQ74DispatchFailed(orderNo, commandPayload, sentAt, e.getMessage());
        }
    }

    private IotCommandAckResultVO sendQ74WithAck(String sn, String payload) {
        R<IotCommandAckResultVO> response = iotServiceClient.sendSetCommandWithAck(sn, Map.of(
                "pointID", properties.getCommandPoint(),
                "value", payload
        ), properties.getCommandMaxAttempts(), properties.getCommandAckTimeoutMs(), internalServiceToken);
        if (response == null || response.getCode() == null || response.getCode() != 200) {
            String message = response != null ? response.getMessage() : "iot-service no response";
            throw new BusinessException(ResultCode.MQTT_COMMAND_FAILED, "Q74 dispatch failed: " + message);
        }
        return response.getData();
    }

    private void markQ74DispatchFailed(String orderNo, String commandPayload, LocalDateTime sentAt, String reason) {
        orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .set(WaterDispenseOrder::getDispenseStatus, DISPENSE_FAILED)
                .set(WaterDispenseOrder::getCommandStatus, COMMAND_FAILED)
                .set(WaterDispenseOrder::getQ74Payload, commandPayload)
                .set(WaterDispenseOrder::getCommandSentAt, sentAt));
        appendEvent(orderNo, "Q74_COMMAND_FAILED", "FAIL", reason, commandPayload);
    }

    private WaterDispenseOrderVO requestRefund(WaterDispenseOrder order, String reason, boolean automatic) {
        validateRefundable(order);

        String refundNo = StringUtils.hasText(order.getRefundNo()) ? order.getRefundNo() : generateRefundNo();
        Long refundAmount = order.getRefundAmount() != null ? order.getRefundAmount() : order.getPayAmount();
        LocalDateTime requestedAt = LocalDateTime.now();

        int locked = orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getId, order.getId())
                .and(w -> w.isNull(WaterDispenseOrder::getRefundStatus)
                        .or().in(WaterDispenseOrder::getRefundStatus, REFUND_NONE, REFUND_FAILED))
                .set(WaterDispenseOrder::getRefundStatus, REFUND_PROCESSING)
                .set(WaterDispenseOrder::getRefundNo, refundNo)
                .set(WaterDispenseOrder::getRefundAmount, refundAmount)
                .set(WaterDispenseOrder::getRefundReason, reason)
                .set(WaterDispenseOrder::getRefundRequestedAt, requestedAt)
                .set(WaterDispenseOrder::getRefundErrorMsg, null));
        if (locked <= 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT,
                    "订单退款状态已变化，请刷新后重试");
        }

        appendEvent(order.getOrderNo(), automatic ? "AUTO_REFUND_REQUEST" : "MANUAL_REFUND_RETRY",
                "SUCCESS", reason, refundNo);
        try {
            JsonNode response = wechatPayClient.createRefund(order.getOrderNo(), refundNo,
                    refundAmount, order.getPayAmount(), reason);
            String wechatRefundId = response.path("refund_id").asText("");
            String wechatStatus = response.path("status").asText("");
            String responseSummary = summarizeWechatRefundResponse(response);

            if ("SUCCESS".equals(wechatStatus)) {
                LocalDateTime successTime = parseWechatTime(response.path("success_time").asText(""));
                markRefundSuccess(order.getOrderNo(), refundNo, wechatRefundId,
                        successTime != null ? successTime : LocalDateTime.now(), responseSummary);
            } else if ("PROCESSING".equals(wechatStatus) || !StringUtils.hasText(wechatStatus)) {
                orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                        .eq(WaterDispenseOrder::getOrderNo, order.getOrderNo())
                        .eq(WaterDispenseOrder::getRefundNo, refundNo)
                        .set(WaterDispenseOrder::getRefundStatus, REFUND_PROCESSING)
                        .set(WaterDispenseOrder::getWechatRefundId, wechatRefundId)
                        .set(WaterDispenseOrder::getRefundErrorMsg, null));
                appendEvent(order.getOrderNo(), "WECHAT_REFUND_ACCEPTED", "SUCCESS",
                        "wechat refund accepted", responseSummary);
            } else {
                markRefundFailed(order.getOrderNo(), refundNo, wechatRefundId,
                        "wechat refund status: " + wechatStatus, responseSummary);
            }
        } catch (Exception e) {
            markRefundFailed(order.getOrderNo(), refundNo, null, e.getMessage(), null);
            throw e;
        }
        return getByOrderNo(order.getOrderNo());
    }

    private void validateRefundable(WaterDispenseOrder order) {
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "扫码订单不存在");
        }
        if (!PAY_SUCCESS.equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "只有已支付订单可以退款");
        }
        if (!DISPENSE_FAILED.equals(order.getDispenseStatus()) || !COMMAND_FAILED.equals(order.getCommandStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "只有出水异常订单可以退款");
        }
        if (!"WECHAT".equals(order.getPaymentProvider()) || Boolean.TRUE.equals(order.getMockPayment())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "只有真实微信支付订单可以自动退款");
        }
        if (!StringUtils.hasText(order.getTransactionId())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "订单缺少微信交易号，不能退款");
        }
        String refundStatus = order.getRefundStatus();
        if (REFUND_SUCCESS.equals(refundStatus) || PAY_REFUND.equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "订单已退款");
        }
        if (REFUND_PROCESSING.equals(refundStatus)) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "订单退款处理中");
        }
    }

    private void markRefundSuccess(String orderNo, String refundNo, String wechatRefundId,
                                   LocalDateTime successAt, String rawData) {
        orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .eq(WaterDispenseOrder::getRefundNo, refundNo)
                .set(WaterDispenseOrder::getPayStatus, PAY_REFUND)
                .set(WaterDispenseOrder::getDispenseStatus, DISPENSE_REFUNDED)
                .set(WaterDispenseOrder::getRefundStatus, REFUND_SUCCESS)
                .set(WaterDispenseOrder::getWechatRefundId, wechatRefundId)
                .set(WaterDispenseOrder::getRefundSuccessAt, successAt)
                .set(WaterDispenseOrder::getRefundErrorMsg, null));
        appendEvent(orderNo, "WECHAT_REFUND_SUCCESS", "SUCCESS", "wechat refund success", rawData);
    }

    private void syncRefundStatus(WaterDispenseOrder order, JsonNode response) {
        String refundNo = response.path("out_refund_no").asText("");
        String orderNo = response.path("out_trade_no").asText("");
        String status = response.path("status").asText("");
        String refundId = response.path("refund_id").asText("");
        long refundAmount = response.path("amount").path("refund").asLong(-1);
        if (!Objects.equals(order.getRefundNo(), refundNo) || !Objects.equals(order.getOrderNo(), orderNo)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat refund query result mismatch");
        }
        if (refundAmount >= 0 && order.getRefundAmount() != null && !Objects.equals(order.getRefundAmount(), refundAmount)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "wechat refund query amount mismatch");
        }

        String summary = summarizeWechatRefundResponse(response);
        if ("SUCCESS".equals(status)) {
            LocalDateTime successTime = parseWechatTime(response.path("success_time").asText(""));
            markRefundSuccess(order.getOrderNo(), order.getRefundNo(), refundId,
                    successTime != null ? successTime : LocalDateTime.now(), summary);
            return;
        }
        if (!"PROCESSING".equals(status)) {
            markRefundFailed(order.getOrderNo(), order.getRefundNo(), refundId,
                    StringUtils.hasText(status) ? "wechat refund status: " + status : "wechat refund status is empty",
                    summary);
        }
    }

    private void markRefundFailed(String orderNo, String refundNo, String wechatRefundId,
                                  String errorMessage, String rawData) {
        String message = StringUtils.hasText(errorMessage) ? errorMessage : "微信退款失败";
        orderMapper.update(null, new LambdaUpdateWrapper<WaterDispenseOrder>()
                .eq(WaterDispenseOrder::getOrderNo, orderNo)
                .eq(WaterDispenseOrder::getRefundNo, refundNo)
                .set(WaterDispenseOrder::getRefundStatus, REFUND_FAILED)
                .set(StringUtils.hasText(wechatRefundId), WaterDispenseOrder::getWechatRefundId, wechatRefundId)
                .set(WaterDispenseOrder::getRefundErrorMsg, message));
        appendEvent(orderNo, "WECHAT_REFUND_FAILED", "FAIL", message, rawData);
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
        Long maxTargetMl = properties.getMaxTargetMl() != null ? properties.getMaxTargetMl() : 100000L;
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
        Integer normalizedWaterType = validateWaterType(waterType);
        long minPayAmount = properties.getMinPayAmount() != null ? properties.getMinPayAmount() : 1L;
        long unitPriceCentsPerLiter = resolveUnitPriceCentsPerLiter(normalizedWaterType);
        if (unitPriceCentsPerLiter <= 0) {
            return minPayAmount;
        }
        long amount = (targetMl * unitPriceCentsPerLiter + 999L) / 1000L;
        return Math.max(minPayAmount, amount);
    }

    private long resolveUnitPriceCentsPerLiter(Integer waterType) {
        Long typePrice = waterType != null && waterType == 1
                ? properties.getHotUnitPriceCentsPerLiter()
                : properties.getColdUnitPriceCentsPerLiter();
        if (typePrice != null) {
            return typePrice;
        }
        return properties.getUnitPriceCentsPerLiter() != null ? properties.getUnitPriceCentsPerLiter() : 0L;
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

    private String summarizeWechatRefundNotify(JsonNode node) {
        return "out_trade_no=" + node.path("out_trade_no").asText("")
                + ", out_refund_no=" + maskTail(node.path("out_refund_no").asText(""))
                + ", refund_id=" + maskTail(node.path("refund_id").asText(""))
                + ", refund_status=" + node.path("refund_status").asText("")
                + ", refund_amount=" + node.path("amount").path("refund").asLong(-1);
    }

    private String summarizeWechatRefundResponse(JsonNode node) {
        return "out_trade_no=" + node.path("out_trade_no").asText("")
                + ", out_refund_no=" + maskTail(node.path("out_refund_no").asText(""))
                + ", refund_id=" + maskTail(node.path("refund_id").asText(""))
                + ", status=" + node.path("status").asText("")
                + ", refund_amount=" + node.path("amount").path("refund").asLong(-1);
    }

    private LocalDateTime parseWechatTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Parse wechat time failed: value={}, message={}", value, e.getMessage());
            return null;
        }
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
        ensureDeviceOnline(device);
        return device;
    }

    private void ensureDeviceOnline(DeviceLookup device) {
        if (device == null || device.getOnlineStatus() == null || device.getOnlineStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "设备不在线，暂不能购买取水");
        }
    }

    private String generateOrderNo() {
        return "WD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String generateRefundNo() {
        return "WR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
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




