package com.platform.water.service;

import com.platform.common.result.PageResult;
import com.platform.water.dto.Q74PreviewDTO;
import com.platform.water.dto.WaterDispenseCreateDTO;
import com.platform.water.dto.WaterDispenseOrderPageQueryDTO;
import com.platform.water.vo.Q74ProtocolVO;
import com.platform.water.vo.WaterDispensePricePreviewVO;
import com.platform.water.vo.WaterDispenseOrderVO;
import com.platform.water.vo.WaterDispenseOrderStatsVO;
import com.platform.water.vo.WaterWechatPayVO;

public interface WaterDispenseService {

    WaterDispenseOrderVO createOrder(WaterDispenseCreateDTO dto);

    WaterDispensePricePreviewVO previewPrice(Long targetMl, Integer waterType);

    WaterWechatPayVO prepareWechatPay(String orderNo);

    void handleWechatPayNotify(String timestamp, String nonce, String signature, String serial, String body);

    void handleWechatRefundNotify(String timestamp, String nonce, String signature, String serial, String body);

    void autoRefundRecentAbnormalOrders();

    void syncProcessingRefunds();

    WaterDispenseOrderVO retryRefund(String orderNo);

    WaterDispenseOrderVO getByOrderNo(String orderNo);

    PageResult<WaterDispenseOrderVO> pageOrders(WaterDispenseOrderPageQueryDTO query);

    WaterDispenseOrderStatsVO getOrderStats(WaterDispenseOrderPageQueryDTO query);

    Q74ProtocolVO previewQ74(Q74PreviewDTO dto);
}
