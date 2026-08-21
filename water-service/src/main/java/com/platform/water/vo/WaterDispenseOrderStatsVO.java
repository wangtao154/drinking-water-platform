package com.platform.water.vo;

import lombok.Data;

@Data
public class WaterDispenseOrderStatsVO {

    private Long total;

    private Long paid;

    private Long pending;

    private Long dispatched;

    private Long sent;

    /** 退款成功订单数。 */
    private Long refunded;

    /** 退款处理中的订单数。 */
    private Long refundProcessing;

    private Long totalAmount;

    /** 退款成功金额合计，单位：分。 */
    private Long refundAmount;

    /** 已支付订单的目标取水量合计，单位：毫升。 */
    private Long totalDispenseMl;
}
