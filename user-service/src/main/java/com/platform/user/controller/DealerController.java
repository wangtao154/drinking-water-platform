package com.platform.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.base.BaseController;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.user.dto.DealerCreateDTO;
import com.platform.user.dto.DealerQueryDTO;
import com.platform.user.dto.DealerUpdateDTO;
import com.platform.user.service.DealerService;
import com.platform.user.vo.DealerOptionVO;
import com.platform.user.vo.DealerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dealers")
@RequiredArgsConstructor
public class DealerController extends BaseController {

    private final DealerService dealerService;

    @PostMapping
    public R<DealerVO> create(@Valid @RequestBody DealerCreateDTO dto) {
        return success(dealerService.create(dto));
    }

    @PutMapping("/{id}")
    public R<DealerVO> update(@PathVariable Long id, @RequestBody DealerUpdateDTO dto) {
        return success(dealerService.update(id, dto));
    }

    @GetMapping("/{id}")
    public R<DealerVO> getById(@PathVariable Long id) {
        return success(dealerService.getById(id));
    }

    @GetMapping
    public R<PageResult<DealerVO>> page(DealerQueryDTO query) {
        IPage<DealerVO> page = dealerService.page(query);
        return pageResult(PageResult.of(page));
    }

    @GetMapping("/options")
    public R<List<DealerOptionVO>> options() {
        return success(dealerService.options());
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dealerService.delete(id);
        return success(null);
    }
}
