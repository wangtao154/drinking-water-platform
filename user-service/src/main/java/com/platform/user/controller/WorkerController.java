package com.platform.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.base.BaseController;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.user.dto.WorkerCreateDTO;
import com.platform.user.dto.WorkerQueryDTO;
import com.platform.user.dto.WorkerUpdateDTO;
import com.platform.user.entity.Worker;
import com.platform.user.service.WorkerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerController extends BaseController {

    private final WorkerService workerService;

    @PostMapping
    public R<Worker> create(@Valid @RequestBody WorkerCreateDTO dto) {
        return success(workerService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Worker> update(@PathVariable Long id, @RequestBody WorkerUpdateDTO dto) {
        return success(workerService.update(id, dto));
    }

    @PutMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id) {
        workerService.resetPassword(id);
        return success(null);
    }

    @GetMapping("/{id}")
    public R<Worker> getById(@PathVariable Long id) {
        return success(workerService.getById(id));
    }

    @GetMapping
    public R<PageResult<Worker>> page(WorkerQueryDTO query) {
        IPage<Worker> page = workerService.page(query);
        return pageResult(PageResult.of(page));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        workerService.delete(id);
        return success(null);
    }
}
