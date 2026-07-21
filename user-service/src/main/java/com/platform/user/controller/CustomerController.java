package com.platform.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.base.BaseController;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.user.dto.CustomerCreateDTO;
import com.platform.user.dto.CustomerQueryDTO;
import com.platform.user.dto.CustomerUpdateDTO;
import com.platform.user.entity.Customer;
import com.platform.user.service.CustomerService;
import com.platform.user.vo.CustomerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController extends BaseController {

    private final CustomerService customerService;

    @PostMapping
    public R<Customer> create(@Valid @RequestBody CustomerCreateDTO dto) {
        return success(customerService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Customer> update(@PathVariable Long id, @RequestBody CustomerUpdateDTO dto) {
        return success(customerService.update(id, dto));
    }

    @GetMapping("/{id}")
    public R<Customer> getById(@PathVariable Long id) {
        return success(customerService.getById(id));
    }

    @GetMapping
    public R<PageResult<CustomerVO>> page(CustomerQueryDTO query) {
        IPage<CustomerVO> page = customerService.page(query);
        return pageResult(PageResult.of(page));
    }

    @GetMapping("/ids")
    public R<List<Long>> findIdsByName(@RequestParam(required = false) String name) {
        if (!StringUtils.hasText(name)) {
            return success(Collections.emptyList());
        }
        return success(customerService.findIdsByName(name));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return success(null);
    }

    /**
     * 重置客户密码为手机号后6位
     */
    @PutMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id) {
        customerService.resetPassword(id);
        return success(null);
    }
}
