package com.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录 DTO
 */
@Data
public class WxLoginDTO {

    /**
     * wx.login() 获取的临时登录凭证 code
     */
    @NotBlank(message = "微信登录code不能为空")
    private String code;

    /**
     * 可选：用户昵称（首次登录时使用）
     */
    private String nickName;

    /**
     * 可选：用户头像URL
     */
    private String avatarUrl;

    /**
     * Mock 模式下的稳定测试 openId。
     * 正式微信登录不会使用该字段。
     */
    private String mockOpenId;
}
