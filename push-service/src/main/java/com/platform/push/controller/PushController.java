package com.platform.push.controller;

import com.platform.common.dto.PageQueryDTO;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.push.service.PushService;
import com.platform.push.vo.PushStatsVO;
import com.platform.push.vo.PushTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 告警推送 Controller
 */
@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    /**
     * 未推送告警分页
     */
    @GetMapping("/unpushed")
    public R<PageResult<PushTaskVO>> unpushedPage(PageQueryDTO query) {
        return R.ok(pushService.unpushedPage(query));
    }

    /**
     * 推送单条告警
     */
    @PostMapping("/alerts/{alertId}")
    public R<Void> pushAlert(@PathVariable Long alertId) {
        pushService.pushAlert(alertId);
        return R.ok();
    }

    /**
     * 批量推送
     */
    @PostMapping("/batch")
    public R<Void> pushBatch() {
        pushService.pushBatch();
        return R.ok();
    }

    /**
     * 推送统计
     */
    @GetMapping("/stats")
    public R<PushStatsVO> getStats() {
        return R.ok(pushService.getStats());
    }
}
