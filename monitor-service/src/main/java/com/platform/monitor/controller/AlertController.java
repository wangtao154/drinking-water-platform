package com.platform.monitor.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.monitor.dto.AlertPageQueryDTO;
import com.platform.monitor.service.MonitorService;
import com.platform.monitor.vo.AlertVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警 Controller（网关路由 /api/v1/alerts/**）
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final MonitorService monitorService;

    /**
     * 分页查询告警
     */
    @GetMapping
    public R<PageResult<AlertVO>> alertPage(AlertPageQueryDTO query) {
        return R.ok(monitorService.alertPage(query));
    }
}
