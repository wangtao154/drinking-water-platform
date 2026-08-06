package com.platform.filter.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.filter.dto.FilterPageQueryDTO;
import com.platform.filter.dto.FilterRegisterBatchDTO;
import com.platform.filter.dto.FilterRegisterDTO;
import com.platform.filter.dto.FilterReplaceDTO;
import com.platform.filter.dto.FilterUpdateDTO;
import com.platform.filter.service.FilterService;
import com.platform.filter.vo.FilterReplaceVO;
import com.platform.filter.vo.FilterStatusLogVO;
import com.platform.filter.vo.FilterVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/filters")
@RequiredArgsConstructor
public class FilterController {

    private final FilterService filterService;

    @PostMapping("/register")
    public R<FilterVO> register(@Valid @RequestBody FilterRegisterDTO dto) {
        return R.ok(filterService.register(dto));
    }

    @PostMapping("/register/batch")
    public R<List<FilterVO>> registerBatch(@Valid @RequestBody FilterRegisterBatchDTO dto) {
        return R.ok(filterService.registerBatch(dto.getFilters()));
    }

    @GetMapping("/{filterId}")
    public R<FilterVO> getByFilterId(@PathVariable String filterId) {
        return R.ok(filterService.getByFilterId(filterId));
    }

    @GetMapping
    public R<PageResult<FilterVO>> page(FilterPageQueryDTO query) {
        return R.ok(filterService.page(query));
    }

    /**
     * 公开接口：游客查询设备滤芯列表（无需登录）
     */
    @GetMapping("/public/device/{deviceId}")
    public R<PageResult<FilterVO>> getPublicDeviceFilters(@PathVariable String deviceId) {
        FilterPageQueryDTO query = new FilterPageQueryDTO();
        query.setCurrentDeviceId(deviceId);
        query.setPageSize(50);
        return R.ok(filterService.page(query));
    }

    @PutMapping("/{filterId}")
    public R<FilterVO> update(@PathVariable String filterId, @RequestBody FilterUpdateDTO dto) {
        return R.ok(filterService.update(filterId, dto));
    }

    @DeleteMapping("/{filterId}")
    public R<Void> delete(@PathVariable String filterId) {
        filterService.delete(filterId);
        return R.ok();
    }

    @GetMapping("/{filterId}/trace")
    public R<List<FilterStatusLogVO>> getLifecycleTrace(@PathVariable String filterId) {
        return R.ok(filterService.getLifecycleTrace(filterId));
    }

    @GetMapping("/device/{deviceId}/replace-history")
    public R<List<FilterReplaceVO>> getReplaceHistory(
            @PathVariable String deviceId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(filterService.getReplaceHistory(deviceId, startTime, endTime));
    }

    /**
     * C 端：滤芯更换
     */
    @PostMapping("/replace")
    public R<FilterReplaceVO> replaceFilter(@Valid @RequestBody FilterReplaceDTO dto) {
        return R.ok(filterService.replaceFilter(dto));
    }
}
