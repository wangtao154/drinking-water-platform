package com.platform.filter.service;

import com.platform.common.result.PageResult;
import com.platform.filter.dto.FilterModelCreateDTO;
import com.platform.filter.dto.FilterModelUpdateDTO;
import com.platform.filter.dto.FilterPageQueryDTO;
import com.platform.filter.vo.FilterModelVO;

import java.util.List;

public interface FilterModelService {

    FilterModelVO create(FilterModelCreateDTO dto);

    FilterModelVO update(Long id, FilterModelUpdateDTO dto);

    void delete(Long id);

    FilterModelVO getById(Long id);

    PageResult<FilterModelVO> page(FilterPageQueryDTO query);

    List<FilterModelVO> listAllEnabled();
}
