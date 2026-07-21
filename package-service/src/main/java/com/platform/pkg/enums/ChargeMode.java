package com.platform.pkg.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计费模式枚举（本地便捷查询，实际定义在 platform-common）
 * 值与 com.platform.common.enums.ChargeMode 保持一致
 */
@Getter
@AllArgsConstructor
public enum ChargeMode {

    FLOW_BASED("按流量"),
    MONTHLY_RENT("月租"),
    PACKAGE_RECHARGE("充值"),
    SHARED("共享计费");

    private final String description;

    /**
     * 根据枚举名获取描述
     */
    public static String getDescriptionByName(String name) {
        if (name == null) return null;
        for (ChargeMode mode : values()) {
            if (mode.name().equals(name)) {
                return mode.description;
            }
        }
        return null;
    }
}
