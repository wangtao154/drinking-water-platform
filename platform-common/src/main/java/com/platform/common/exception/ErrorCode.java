package com.platform.common.exception;

/**
 * 错误码接口
 * 自定义异常可实现此接口以规范化错误码输出
 */
public interface ErrorCode {

    Integer getCode();

    String getMessage();
}
