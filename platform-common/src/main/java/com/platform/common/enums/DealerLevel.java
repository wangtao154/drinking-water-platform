package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 经销商层级
 */
@Getter
@AllArgsConstructor
public enum DealerLevel {
    L1_DEALER("一级经销商（省级）"),
    L2_DEALER("二级经销商（市级）"),
    L3_DEALER("三级经销商（区县级）");

    private final String description;
}
