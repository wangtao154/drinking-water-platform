package com.platform.auth.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {

    private String accessToken;

    private String refreshToken;

    /** access token 过期时间（秒） */
    private Long expiresIn;

    private UserInfoVO userInfo;

    /** 是否首次登录（需修改密码） */
    private Boolean firstLogin;
}
