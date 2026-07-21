package com.platform.filter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.UserContext;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.common.util.QRCodeUtil;
import com.platform.common.util.SnowflakeIdUtil;
import com.platform.filter.dto.FilterPageQueryDTO;
import com.platform.filter.dto.FilterRegisterDTO;
import com.platform.filter.dto.FilterReplaceDTO;
import com.platform.filter.dto.FilterUpdateDTO;
import com.platform.filter.entity.FilterInstance;
import com.platform.filter.entity.FilterModel;
import com.platform.filter.entity.FilterReplaceLog;
import com.platform.filter.entity.FilterStatusLog;
import com.platform.filter.mapper.FilterInstanceMapper;
import com.platform.filter.mapper.FilterModelMapper;
import com.platform.filter.mapper.FilterReplaceLogMapper;
import com.platform.filter.mapper.FilterStatusLogMapper;
import com.platform.filter.service.FilterService;
import com.platform.filter.vo.FilterReplaceVO;
import com.platform.filter.vo.FilterStatusLogVO;
import com.platform.filter.vo.FilterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilterServiceImpl implements FilterService {

    private final FilterInstanceMapper filterInstanceMapper;
    private final FilterModelMapper filterModelMapper;
    private final FilterStatusLogMapper filterStatusLogMapper;
    private final FilterReplaceLogMapper filterReplaceLogMapper;
    private final SnowflakeIdUtil snowflakeIdUtil;

    private static final AtomicInteger FILTER_SEQ = new AtomicInteger(0);

    @PostConstruct
    public void initSeq() {
        // 启动时从数据库加载当前最大序号，避免重启后序列号冲突
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String prefix = "FL" + yearMonth;
        LambdaQueryWrapper<FilterInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FilterInstance::getFilterId, prefix)
               .orderByDesc(FilterInstance::getFilterId)
               .last("LIMIT 1");
        List<FilterInstance> lastList = filterInstanceMapper.selectList(wrapper);
        if (!lastList.isEmpty()) {
            String lastFilterId = lastList.get(0).getFilterId();
            try {
                int lastSeq = Integer.parseInt(lastFilterId.substring(prefix.length()));
                FILTER_SEQ.set(lastSeq);
                log.info("[FilterService] 初始化序号: {} (lastFilterId={})", lastSeq, lastFilterId);
            } catch (NumberFormatException e) {
                log.warn("[FilterService] 无法解析序号: {}", lastFilterId);
            }
        }
    }

    @Override
    @Transactional
    public FilterVO register(FilterRegisterDTO dto) {
        // 校验型号是否存在
        FilterModel model = filterModelMapper.selectById(dto.getFilterModelId());
        if (model == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "滤芯型号不存在");
        }

        FilterInstance instance = new FilterInstance();
        instance.setFilterModelId(dto.getFilterModelId());
        instance.setProductionDate(dto.getProductionDate());
        instance.setProductionBatch(dto.getProductionBatch());
        instance.setLifecycleStatus("IN_STOCK");

        // 生成 filterId: FL + 年月 + 6位序号
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        int seq = FILTER_SEQ.incrementAndGet() % 1000000;
        String filterId = "FL" + yearMonth + String.format("%06d", seq);
        instance.setFilterId(filterId);

        // 生成二维码
        String qrContent = "FILTER:" + filterId;
        String qrCodeBase64 = QRCodeUtil.generateBase64(qrContent);
        instance.setQrCodeUrl(qrCodeBase64);

        filterInstanceMapper.insert(instance);
        log.info("[FilterService] 登记滤芯: filterId={}", filterId);

        // 记录状态变更日志
        logStatusChange(filterId, null, "IN_STOCK", "滤芯登记");

        return toVO(instance, model);
    }

    @Override
    @Transactional
    public List<FilterVO> registerBatch(List<FilterRegisterDTO> dtoList) {
        return dtoList.stream()
                .map(this::register)
                .collect(Collectors.toList());
    }

    @Override
    public FilterVO getByFilterId(String filterId) {
        FilterInstance instance = filterInstanceMapper.selectOne(
                new LambdaQueryWrapper<FilterInstance>()
                        .eq(FilterInstance::getFilterId, filterId)
        );
        if (instance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "滤芯不存在");
        }

        FilterModel model = filterModelMapper.selectById(instance.getFilterModelId());
        return toVO(instance, model);
    }

    @Override
    public PageResult<FilterVO> page(FilterPageQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<FilterInstance> wrapper = new LambdaQueryWrapper<>();
        if (query.getModelId() != null) {
            wrapper.eq(FilterInstance::getFilterModelId, query.getModelId());
        }
        if (StringUtils.hasText(query.getLifecycleStatus())) {
            wrapper.eq(FilterInstance::getLifecycleStatus, query.getLifecycleStatus());
        } else {
            // 默认排除已报废滤芯
            wrapper.ne(FilterInstance::getLifecycleStatus, "SCRAPPED");
        }
        if (StringUtils.hasText(query.getCurrentDeviceId())) {
            wrapper.eq(FilterInstance::getCurrentDeviceId, query.getCurrentDeviceId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(FilterInstance::getFilterId, query.getKeyword());
        }
        if (query.getCreatedAtStart() != null) {
            wrapper.ge(FilterInstance::getCreatedAt, query.getCreatedAtStart());
        }
        if (query.getCreatedAtEnd() != null) {
            wrapper.le(FilterInstance::getCreatedAt, query.getCreatedAtEnd());
        }
        if (query.getInstalledAtStart() != null) {
            wrapper.ge(FilterInstance::getInstalledAt, query.getInstalledAtStart());
        }
        if (query.getInstalledAtEnd() != null) {
            wrapper.le(FilterInstance::getInstalledAt, query.getInstalledAtEnd());
        }
        wrapper.orderByDesc(FilterInstance::getCreatedAt);

        IPage<FilterInstance> page = new Page<>(query.getPageNum(), query.getPageSize());
        filterInstanceMapper.selectPage(page, wrapper);

        return PageResult.of(page.convert(inst -> {
            FilterModel model = filterModelMapper.selectById(inst.getFilterModelId());
            return toVO(inst, model);
        }));
    }

    @Override
    public List<FilterStatusLogVO> getLifecycleTrace(String filterId) {
        List<FilterStatusLog> logs = filterStatusLogMapper.selectList(
                new LambdaQueryWrapper<FilterStatusLog>()
                        .eq(FilterStatusLog::getFilterId, filterId)
                        .orderByAsc(FilterStatusLog::getCreatedAt)
        );
        return logs.stream().map(this::toStatusLogVO).collect(Collectors.toList());
    }

    @Override
    public List<FilterReplaceVO> getReplaceHistory(String deviceId, String startTime, String endTime) {
        LambdaQueryWrapper<FilterReplaceLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FilterReplaceLog::getDeviceId, deviceId);
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(FilterReplaceLog::getReplacedAt, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(FilterReplaceLog::getReplacedAt, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(FilterReplaceLog::getCreatedAt);

        List<FilterReplaceLog> logs = filterReplaceLogMapper.selectList(wrapper);
        return logs.stream().map(this::toReplaceVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FilterVO update(String filterId, FilterUpdateDTO dto) {
        FilterInstance instance = filterInstanceMapper.selectOne(
                new LambdaQueryWrapper<FilterInstance>()
                        .eq(FilterInstance::getFilterId, filterId)
        );
        if (instance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "滤芯不存在");
        }

        String oldStatus = instance.getLifecycleStatus();

        if (dto.getFilterModelId() != null) {
            FilterModel model = filterModelMapper.selectById(dto.getFilterModelId());
            if (model == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "滤芯型号不存在");
            }
            instance.setFilterModelId(dto.getFilterModelId());
        }
        if (dto.getProductionDate() != null) {
            instance.setProductionDate(dto.getProductionDate());
        }
        if (dto.getProductionBatch() != null) {
            instance.setProductionBatch(dto.getProductionBatch());
        }
        if (dto.getRemark() != null) {
            instance.setRemark(dto.getRemark());
        }
        if (dto.getLifecycleStatus() != null && !dto.getLifecycleStatus().equals(oldStatus)) {
            instance.setLifecycleStatus(dto.getLifecycleStatus());
            if ("SCRAPPED".equals(dto.getLifecycleStatus())) {
                instance.setScrappedAt(LocalDateTime.now());
            }
            // 记录状态变更日志
            logStatusChange(filterId, oldStatus, dto.getLifecycleStatus(), "手动修改状态");
        }

        filterInstanceMapper.updateById(instance);
        log.info("[FilterService] 更新滤芯: filterId={}", filterId);

        FilterModel model = filterModelMapper.selectById(instance.getFilterModelId());
        return toVO(instance, model);
    }

    @Override
    @Transactional
    public void delete(String filterId) {
        FilterInstance instance = filterInstanceMapper.selectOne(
                new LambdaQueryWrapper<FilterInstance>()
                        .eq(FilterInstance::getFilterId, filterId)
        );
        if (instance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "滤芯不存在");
        }

        // 使用中的滤芯不允许删除
        if ("IN_USE".equals(instance.getLifecycleStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "使用中的滤芯不允许删除，请先更换");
        }

        filterInstanceMapper.deleteById(instance.getId());
        log.info("[FilterService] 删除滤芯: filterId={}", filterId);

        // 记录状态变更日志
        logStatusChange(filterId, instance.getLifecycleStatus(), "DELETED", "逻辑删除");
    }

    /**
     * C 端：滤芯更换
     * 1. 旧滤芯报废（清除设备关联）
     * 2. 新滤芯安装到设备
     * 3. 记录换芯日志
     */
    @Override
    @Transactional
    public FilterReplaceVO replaceFilter(FilterReplaceDTO dto) {
        // 1. 校验新滤芯存在且在库
        FilterInstance newFilter = filterInstanceMapper.selectOne(
                new LambdaQueryWrapper<FilterInstance>()
                        .eq(FilterInstance::getFilterId, dto.getNewFilterId())
        );
        if (newFilter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "新滤芯不存在");
        }
        if (!"IN_STOCK".equals(newFilter.getLifecycleStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "新滤芯状态非在库，无法安装");
        }

        // 2. 旧滤芯处理（如果存在）
        FilterInstance oldFilter = null;
        if (dto.getOldFilterId() != null && !dto.getOldFilterId().isEmpty()) {
            oldFilter = filterInstanceMapper.selectOne(
                    new LambdaQueryWrapper<FilterInstance>()
                            .eq(FilterInstance::getFilterId, dto.getOldFilterId())
            );
            if (oldFilter == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "旧滤芯不存在");
            }
            if (!"IN_USE".equals(oldFilter.getLifecycleStatus())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "旧滤芯状态非使用中");
            }

            // 报废旧滤芯（使用 LambdaUpdateWrapper 确保 currentDeviceId 被清空为 null）
            String oldStatus = oldFilter.getLifecycleStatus();
            LambdaUpdateWrapper<FilterInstance> oldFilterUpdate = new LambdaUpdateWrapper<>();
            oldFilterUpdate.eq(FilterInstance::getFilterId, dto.getOldFilterId())
                    .set(FilterInstance::getLifecycleStatus, "SCRAPPED")
                    .set(FilterInstance::getScrappedAt, LocalDateTime.now())
                    .set(FilterInstance::getCurrentDeviceId, null);
            filterInstanceMapper.update(null, oldFilterUpdate);
            logStatusChange(dto.getOldFilterId(), oldStatus, "SCRAPPED", "滤芯更换-旧滤芯报废");
        }

        // 3. 安装新滤芯
        String newOldStatus = newFilter.getLifecycleStatus();
        newFilter.setLifecycleStatus("IN_USE");
        newFilter.setCurrentDeviceId(dto.getDeviceId());
        newFilter.setInstalledAt(LocalDateTime.now());

        CurrentUser currentUser = UserContext.get();
        if (currentUser != null) {
            newFilter.setInstalledBy(currentUser.getUserId());
        }
        filterInstanceMapper.updateById(newFilter);
        logStatusChange(dto.getNewFilterId(), newOldStatus, "IN_USE", "滤芯更换-新滤芯安装");

        // 4. 创建换芯记录
        FilterReplaceLog replaceLog = new FilterReplaceLog();
        replaceLog.setDeviceId(dto.getDeviceId());
        replaceLog.setOldFilterId(dto.getOldFilterId());
        replaceLog.setNewFilterId(dto.getNewFilterId());
        if (currentUser != null) {
            replaceLog.setReplacedBy(currentUser.getUserId());
        }
        replaceLog.setReplacedAt(LocalDateTime.now());
        if (oldFilter != null) {
            replaceLog.setOldUsedDuration(oldFilter.getUsedDuration());
            replaceLog.setOldUsedFlow(oldFilter.getUsedFlow());
        }
        replaceLog.setRemark(dto.getRemark());
        filterReplaceLogMapper.insert(replaceLog);

        log.info("[FilterService] 滤芯更换: device={}, old={}, new={}",
                dto.getDeviceId(), dto.getOldFilterId(), dto.getNewFilterId());

        return toReplaceVO(replaceLog);
    }

    private void logStatusChange(String filterId, String fromStatus, String toStatus, String remark) {
        FilterStatusLog statusLog = new FilterStatusLog();
        statusLog.setFilterId(filterId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setRemark(remark);

        CurrentUser currentUser = UserContext.get();
        if (currentUser != null) {
            statusLog.setOperatorId(currentUser.getUserId());
            statusLog.setOperatorName(currentUser.getUserName());
        }

        filterStatusLogMapper.insert(statusLog);
    }

    private FilterVO toVO(FilterInstance instance, FilterModel model) {
        FilterVO vo = new FilterVO();
        BeanUtils.copyProperties(instance, vo);

        if (model != null) {
            vo.setModelName(model.getModelName());
            // 计算剩余寿命百分比
            vo.setRemainPercentage(calculateRemainPercentage(instance, model));
        }

        return vo;
    }

    private Integer calculateRemainPercentage(FilterInstance instance, FilterModel model) {
        if (model.getStandardLifeDuration() == null || model.getStandardLifeDuration() <= 0) {
            return null;
        }
        // 按时间和流量综合计算，取较大损耗值
        long durationMax = model.getStandardLifeDuration() * 30 * 24 * 3600L; // 月转秒
        long flowMax = model.getStandardLifeFlow();

        int durationPercent = 100;
        int flowPercent = 100;

        if (durationMax > 0 && instance.getUsedDuration() != null && instance.getUsedDuration() > 0) {
            durationPercent = Math.max(0, 100 - (int) (instance.getUsedDuration() * 100 / durationMax));
        }
        if (flowMax > 0 && instance.getUsedFlow() != null && instance.getUsedFlow() > 0) {
            flowPercent = Math.max(0, 100 - (int) (instance.getUsedFlow() * 100 / flowMax));
        }

        return Math.min(durationPercent, flowPercent);
    }

    private FilterStatusLogVO toStatusLogVO(FilterStatusLog entity) {
        FilterStatusLogVO vo = new FilterStatusLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private FilterReplaceVO toReplaceVO(FilterReplaceLog entity) {
        FilterReplaceVO vo = new FilterReplaceVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
