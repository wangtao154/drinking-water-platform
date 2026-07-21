package com.platform.filter.service;

import com.platform.common.result.PageResult;
import com.platform.filter.dto.FilterPageQueryDTO;
import com.platform.filter.dto.FilterRegisterDTO;
import com.platform.filter.dto.FilterReplaceDTO;
import com.platform.filter.dto.FilterUpdateDTO;
import com.platform.filter.vo.FilterReplaceVO;
import com.platform.filter.vo.FilterStatusLogVO;
import com.platform.filter.vo.FilterVO;

import java.util.List;

public interface FilterService {

    FilterVO register(FilterRegisterDTO dto);

    List<FilterVO> registerBatch(List<FilterRegisterDTO> dtoList);

    FilterVO getByFilterId(String filterId);

    PageResult<FilterVO> page(FilterPageQueryDTO query);

    FilterVO update(String filterId, FilterUpdateDTO dto);

    void delete(String filterId);

    List<FilterStatusLogVO> getLifecycleTrace(String filterId);

    List<FilterReplaceVO> getReplaceHistory(String deviceId, String startTime, String endTime);

    /**
     * C 端：滤芯更换（旧滤芯报废 + 新滤芯安装 + 记录换芯日志）
     */
    FilterReplaceVO replaceFilter(FilterReplaceDTO dto);
}
