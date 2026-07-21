package com.platform.user.service;

import com.platform.user.vo.OfficialAccountBindStatusVO;
import com.platform.user.vo.OfficialAccountBindUrlVO;

public interface WechatOfficialBindingService {

    OfficialAccountBindUrlVO createBindUrl(Long workerId);

    OfficialAccountBindStatusVO getBindStatus(Long workerId);

    void handleCallback(String code, String state);
}
