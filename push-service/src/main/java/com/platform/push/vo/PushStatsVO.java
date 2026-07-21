package com.platform.push.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 推送统计 VO
 */
@Data
public class PushStatsVO implements Serializable {

    /**
     * 已推送数量
     */
    private Long totalPushed;

    /**
     * 未推送数量
     */
    private Long totalUnpushed;

    /**
     * 推送失败数量
     */
    private Long totalFailed;
}
