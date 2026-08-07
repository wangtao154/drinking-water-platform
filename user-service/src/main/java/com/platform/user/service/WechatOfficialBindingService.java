package com.platform.user.service;

import com.platform.user.vo.OfficialAccountBindStatusVO;
import com.platform.user.vo.OfficialAccountBindUrlVO;

import java.util.List;
import java.util.Map;

public interface WechatOfficialBindingService {

    OfficialAccountBindUrlVO createBindUrl(Long workerId);

    OfficialAccountBindStatusVO getBindStatus(Long workerId);

    void handleCallback(String code, String state);

    String verifyEventCallback(String signature, String timestamp, String nonce, String echostr);

    /**
     * 处理公众号事件/消息回调，返回回复文本（null 表示不回复）
     */
    String handleEventCallback(String signature, String timestamp, String nonce, String body);

    /**
     * 拉取公众号全部粉丝列表（含昵称、头像）
     */
    List<Map<String, Object>> getFollowers();

    /**
     * 获取已绑定的管理员公众号 openid 列表
     */
    List<String> getAdminOpenIds();

    /**
     * 移除一个管理员 openid
     */
    void removeAdminOpenId(String openid);
}
