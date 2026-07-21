package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户主体类型
 */
@Getter
@AllArgsConstructor
public enum CustomerType {
    PERSONAL("个人"),
    FAMILY("家庭"),
    COMPANY("公司"),
    SCHOOL("学校"),
    OTHER_ORG("其他单位");

    private final String description;
}
