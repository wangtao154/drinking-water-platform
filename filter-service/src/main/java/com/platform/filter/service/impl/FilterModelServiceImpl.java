package com.platform.filter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.common.util.SnowflakeIdUtil;
import com.platform.filter.dto.FilterModelCreateDTO;
import com.platform.filter.dto.FilterModelUpdateDTO;
import com.platform.filter.dto.FilterPageQueryDTO;
import com.platform.filter.entity.FilterModel;
import com.platform.filter.mapper.FilterModelMapper;
import com.platform.filter.service.FilterModelService;
import com.platform.filter.vo.FilterModelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilterModelServiceImpl implements FilterModelService {

    private final FilterModelMapper filterModelMapper;
    private final SnowflakeIdUtil snowflakeIdUtil;

    private static final AtomicInteger CODE_SEQUENCE = new AtomicInteger(0);

    @Override
    public FilterModelVO create(FilterModelCreateDTO dto) {
        FilterModel model = new FilterModel();
        BeanUtils.copyProperties(dto, model);

        // 生成型号编码: FM + 8位序号
        long seq = snowflakeIdUtil.nextId() % 100000000;
        model.setModelCode("FM" + String.format("%08d", seq));
        model.setStatus("ENABLED");

        filterModelMapper.insert(model);
        log.info("[FilterModelService] 创建滤芯型号: modelCode={}", model.getModelCode());

        return toVO(model);
    }

    @Override
    public FilterModelVO update(Long id, FilterModelUpdateDTO dto) {
        FilterModel existing = filterModelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (StringUtils.hasText(dto.getModelName())) {
            existing.setModelName(dto.getModelName());
        }
        if (StringUtils.hasText(dto.getCategory())) {
            existing.setCategory(dto.getCategory());
        }
        if (dto.getFilterLevel() != null) {
            existing.setFilterLevel(dto.getFilterLevel());
        }
        if (dto.getStandardLifeDuration() != null) {
            existing.setStandardLifeDuration(dto.getStandardLifeDuration());
        }
        if (dto.getStandardLifeFlow() != null) {
            existing.setStandardLifeFlow(dto.getStandardLifeFlow());
        }
        if (dto.getPrice() != null) {
            existing.setPrice(dto.getPrice());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            existing.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            existing.setStatus(dto.getStatus());
        }

        filterModelMapper.updateById(existing);
        log.info("[FilterModelService] 更新滤芯型号: id={}", id);

        return toVO(existing);
    }

    @Override
    public void delete(Long id) {
        FilterModel existing = filterModelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        filterModelMapper.deleteById(id);
        log.info("[FilterModelService] 删除滤芯型号: id={}", id);
    }

    @Override
    public FilterModelVO getById(Long id) {
        FilterModel model = filterModelMapper.selectById(id);
        if (model == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toVO(model);
    }

    @Override
    public PageResult<FilterModelVO> page(FilterPageQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<FilterModel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(FilterModel::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(FilterModel::getCategory, query.getCategory());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(FilterModel::getModelCode, query.getKeyword())
                    .or().like(FilterModel::getModelName, query.getKeyword()));
        }
        wrapper.orderByDesc(FilterModel::getCreatedAt);

        IPage<FilterModel> page = new Page<>(query.getPageNum(), query.getPageSize());
        filterModelMapper.selectPage(page, wrapper);

        return PageResult.of(page.convert(this::toVO));
    }

    @Override
    public List<FilterModelVO> listAllEnabled() {
        List<FilterModel> models = filterModelMapper.selectList(
                new LambdaQueryWrapper<FilterModel>()
                        .eq(FilterModel::getStatus, "ENABLED")
        );
        return models.stream().map(this::toVO).collect(Collectors.toList());
    }

    private FilterModelVO toVO(FilterModel entity) {
        FilterModelVO vo = new FilterModelVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
