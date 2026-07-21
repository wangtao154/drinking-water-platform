package com.platform.auth.service;

import com.platform.auth.dto.ChangePasswordDTO;
import com.platform.auth.dto.CustomerChangePasswordDTO;
import com.platform.auth.dto.CustomerLoginDTO;
import com.platform.auth.dto.LoginDTO;
import com.platform.auth.dto.WxLoginDTO;
import com.platform.auth.vo.LoginVO;
import com.platform.auth.vo.UserInfoVO;

public interface AuthService {

    /**
     * 账号密码登录
     */
    LoginVO login(LoginDTO dto, String clientIp);

    /**
     * 微信小程序登录（Mock 模式：无真实 AppID 时用 code 生成假 openid）
     */
    LoginVO wxLogin(WxLoginDTO dto, String clientIp);

    /**
     * 刷新 Token
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 登出
     */
    void logout(Long userId, String accessToken);

    /**
     * 获取当前用户信息
     */
    UserInfoVO getCurrentUserInfo(Long userId);

    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /**
     * 客户手机号密码登录
     */
    LoginVO customerLogin(CustomerLoginDTO dto, String clientIp);

    /**
     * 客户修改密码（首次登录或日常修改）
     */
    void customerChangePassword(Long customerId, CustomerChangePasswordDTO dto);

    /**
     * 运维人员手机号密码登录
     */
    LoginVO workerLogin(CustomerLoginDTO dto, String clientIp);

    /**
     * 运维人员修改密码（首次登录或日常修改）
     */
    void workerChangePassword(Long workerId, CustomerChangePasswordDTO dto);
}
