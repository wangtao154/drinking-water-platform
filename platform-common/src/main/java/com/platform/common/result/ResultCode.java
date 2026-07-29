package com.platform.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Unified result codes used by platform services.
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // Success
    SUCCESS(200, "success"),

    // Client errors: device and business validation, 400xx
    DEVICE_NOT_FOUND(40001, "设备ID不存在"),
    DEVICE_NOT_REGISTERED(40002, "设备尚未登记，请先联系厂家录入设备信息"),
    DEVICE_ALREADY_BOUND(40003, "该设备已被其他用户绑定"),
    FILTER_NOT_FOUND(40004, "滤芯ID不存在"),
    PACKAGE_NOT_FOUND(40005, "套餐不存在或已禁用"),
    DEALER_NOT_FOUND(40006, "经销商不存在或已禁用"),
    PARAM_INVALID(40007, "参数校验失败"),
    BIND_IN_PROGRESS(40008, "设备绑定中，请稍候"),
    DEVICE_OFFLINE(40009, "设备离线，无法下发指令"),
    FILTER_ALREADY_IN_USE(40010, "滤芯已被安装使用"),

    // Authentication, 401xx
    UNAUTHORIZED(40100, "未登录或Token已过期"),
    ACCOUNT_NOT_FOUND(40101, "账号不存在"),
    PASSWORD_ERROR(40102, "密码错误"),
    ACCOUNT_DISABLED(40103, "账号已被禁用"),
    TOKEN_INVALID(40104, "Token无效"),
    REFRESH_TOKEN_EXPIRED(40105, "刷新Token已过期，请重新登录"),

    // Authorization, 403xx
    FORBIDDEN(40300, "无权限访问"),

    // Resource, 404xx
    NOT_FOUND(40400, "资源不存在"),
    WORK_ORDER_NOT_FOUND(40401, "工单不存在"),
    ORDER_NOT_FOUND(40402, "订单不存在"),

    // Conflict, 409xx
    DEVICE_STATUS_CONFLICT(40901, "设备状态不允许此操作"),
    WORK_ORDER_STATUS_CONFLICT(40902, "工单状态不允许此操作"),
    ORDER_STATUS_CONFLICT(40903, "订单状态不允许此操作"),
    BALANCE_INSUFFICIENT(40904, "余额不足"),
    FILTER_LIFE_EXPIRED(40905, "滤芯已过期"),
    DATA_DUPLICATE(40906, "数据已存在"),

    // Server errors, 5xxxx
    INTERNAL_ERROR(50000, "服务器内部错误"),
    MQTT_COMMAND_FAILED(50001, "MQTT指令下发失败"),
    PAYMENT_FAILED(50002, "支付下单失败"),
    FILE_UPLOAD_FAILED(50003, "文件上传失败"),
    INFLUXDB_ERROR(50004, "时序数据库操作失败"),
    RABBITMQ_ERROR(50005, "消息队列操作失败"),
    REDIS_ERROR(50006, "缓存操作失败");

    private final Integer code;
    private final String message;
}
