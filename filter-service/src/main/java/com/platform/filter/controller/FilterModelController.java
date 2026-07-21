package com.platform.filter.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.filter.dto.FilterModelCreateDTO;
import com.platform.filter.dto.FilterModelUpdateDTO;
import com.platform.filter.dto.FilterPageQueryDTO;
import com.platform.filter.service.FilterModelService;
import com.platform.filter.vo.FilterModelVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/filter-models")
@RequiredArgsConstructor
public class FilterModelController {

    private final FilterModelService filterModelService;

    @PostMapping
    public R<FilterModelVO> create(@Valid @RequestBody FilterModelCreateDTO dto) {
        return R.ok(filterModelService.create(dto));
    }

    @PutMapping("/{id}")
    public R<FilterModelVO> update(@PathVariable Long id, @RequestBody FilterModelUpdateDTO dto) {
        return R.ok(filterModelService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        filterModelService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<FilterModelVO> getById(@PathVariable Long id) {
        return R.ok(filterModelService.getById(id));
    }

    @GetMapping
    public R<PageResult<FilterModelVO>> page(FilterPageQueryDTO query) {
        return R.ok(filterModelService.page(query));
    }

    @GetMapping("/enabled")
    public R<List<FilterModelVO>> listAllEnabled() {
        return R.ok(filterModelService.listAllEnabled());
    }
}
