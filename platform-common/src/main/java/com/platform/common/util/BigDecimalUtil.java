package com.platform.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额计算工具
 * 统一使用「分」为单位（Long），避免浮点精度问题
 * 与前端交互时按需转换为「元」
 */
public class BigDecimalUtil {

    private static final int SCALE = 2;

    private BigDecimalUtil() {
    }

    // ==================== 分 <-> 元 转换 ====================

    /**
     * 分 → 元（BigDecimal）
     */
    public static BigDecimal fenToYuan(Long fen) {
        if (fen == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(fen).movePointLeft(2).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 元 → 分（Long）
     */
    public static Long yuanToFen(BigDecimal yuan) {
        if (yuan == null) return 0L;
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 元字符串 → 分（Long）
     */
    public static Long yuanStrToFen(String yuanStr) {
        if (yuanStr == null || yuanStr.isEmpty()) return 0L;
        return yuanToFen(new BigDecimal(yuanStr));
    }

    // ==================== 加减乘除 ====================

    /**
     * 加法（分）
     */
    public static Long add(Long a, Long b) {
        return (a == null ? 0L : a) + (b == null ? 0L : b);
    }

    /**
     * 减法（分）
     */
    public static Long subtract(Long a, Long b) {
        return (a == null ? 0L : a) - (b == null ? 0L : b);
    }

    /**
     * 乘法：金额 × 比例
     *
     * @param amount 金额（分）
     * @param rate   比例（0.0000~1.0000）
     * @return 结果（分，四舍五入）
     */
    public static Long multiply(Long amount, BigDecimal rate) {
        if (amount == null || rate == null) return 0L;
        return BigDecimal.valueOf(amount)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * 按比例分摊金额（确保分摊之和等于原金额）
     *
     * @param totalAmount 总金额（分）
     * @param rates       各分摊比例
     * @return 各分摊金额数组（分），最后一位用 total - 前面之和 计算
     */
    public static Long[] allocate(Long totalAmount, BigDecimal[] rates) {
        if (totalAmount == null || rates == null || rates.length == 0) {
            return new Long[0];
        }

        Long[] result = new Long[rates.length];
        long allocated = 0L;

        for (int i = 0; i < rates.length - 1; i++) {
            result[i] = multiply(totalAmount, rates[i]);
            allocated += result[i];
        }
        // 最后一份 = 总额 - 已分摊
        result[rates.length - 1] = totalAmount - allocated;

        return result;
    }
}
