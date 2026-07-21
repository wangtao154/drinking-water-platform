package com.platform.common.base;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用控制器基类
 * 提供统一的响应封装方法
 */
@RestController
public abstract class BaseController {

    /**
     * 成功响应
     */
    protected <T> R<T> success() {
        return R.ok();
    }

    protected <T> R<T> success(T data) {
        return R.ok(data);
    }

    protected <T> R<T> success(T data, String message) {
        return R.ok(data, message);
    }

    /**
     * 分页响应
     */
    protected <T> R<PageResult<T>> pageResult(PageResult<T> page) {
        return R.ok(page);
    }

    /**
     * 失败响应
     */
    protected <T> R<T> failure(String message) {
        return R.fail(com.platform.common.result.ResultCode.INTERNAL_ERROR, message);
    }
}
