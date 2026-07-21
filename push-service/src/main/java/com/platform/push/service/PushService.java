package com.platform.push.service;

import com.platform.common.dto.PageQueryDTO;
import com.platform.common.result.PageResult;
import com.platform.push.vo.PushStatsVO;
import com.platform.push.vo.PushTaskVO;

/**
 * 告警推送 Service
 */
public interface PushService {

    /**
     * 未推送告警分页
     */
    PageResult<PushTaskVO> unpushedPage(PageQueryDTO query);

    /**
     * 推送单条告警
     */
    void pushAlert(Long alertId);

    /**
     * 批量推送所有未推送告警
     */
    void pushBatch();

    /**
     * 推送统计
     */
    PushStatsVO getStats();
}
