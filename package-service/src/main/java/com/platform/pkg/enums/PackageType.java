package com.platform.pkg.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 套餐类型
 */
@Getter
@AllArgsConstructor
public enum PackageType {

    QR_SCAN("扫码套餐"),
    SHARED("共享水机"),
    RENTAL("租赁"),
    WALLET("项目钱包"),
    INSTALL("安装服务");

    private final String description;

    /**
     * 根据枚举名获取描述
     */
    public static String getDescriptionByName(String name) {
        if (name == null) return null;
        for (PackageType type : values()) {
            if (type.name().equals(name)) {
                return type.description;
            }
        }
        return null;
    }
}
