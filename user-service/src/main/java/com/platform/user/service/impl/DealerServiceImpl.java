package com.platform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.enums.DealerLevel;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.platform.user.dto.DealerCreateDTO;
import com.platform.user.dto.DealerQueryDTO;
import com.platform.user.dto.DealerUpdateDTO;
import com.platform.user.entity.Dealer;
import com.platform.user.mapper.DealerMapper;
import com.platform.user.service.DealerService;
import com.platform.user.vo.DealerOptionVO;
import com.platform.user.vo.DealerVO;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DealerServiceImpl implements DealerService {

    private final DealerMapper dealerMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public DealerVO create(DealerCreateDTO dto) {
        Dealer dealer = new Dealer();
        dealer.setDealerCode(dto.getDealerCode());
        dealer.setDealerName(dto.getDealerName());
        dealer.setParentId(dto.getParentId());
        dealer.setContactName(dto.getContactPerson());
        dealer.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone() : dto.getContactPhone());
        dealer.setPassword(passwordEncoder.encode(defaultPassword(dto)));
        dealer.setDealerLevel(convertLevel(dto.getLevel()));
        dealer.setCommissionRate(dto.getCommissionRate());
        dealer.setRegion(dto.getAddress());
        dealer.setStatus("ACTIVE");
        dealer.setRegisteredAt(LocalDateTime.now());
        dealer.setRechargeAmount(0L);
        dealer.setCreditBalance(0L);
        dealer.setTotalBalance(0L);
        dealerMapper.insert(dealer);
        return toVO(dealer);
    }

    @Override
    @Transactional
    public DealerVO update(Long id, DealerUpdateDTO dto) {
        Dealer dealer = dealerMapper.selectById(id);
        if (dealer == null) {
            throw new BusinessException(ResultCode.DEALER_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getDealerName())) {
            dealer.setDealerName(dto.getDealerName());
        }
        if (dto.getContactPerson() != null) {
            dealer.setContactName(dto.getContactPerson());
        }
        if (StringUtils.hasText(dto.getContactPhone())) {
            dealer.setPhone(dto.getContactPhone());
        }
        if (dto.getAddress() != null) {
            dealer.setRegion(dto.getAddress());
        }
        if (dto.getLevel() != null) {
            dealer.setDealerLevel(convertLevel(dto.getLevel()));
        }
        dealerMapper.updateById(dealer);
        return toVO(dealer);
    }

    @Override
    public DealerVO getById(Long id) {
        Dealer dealer = dealerMapper.selectById(id);
        if (dealer == null) {
            throw new BusinessException(ResultCode.DEALER_NOT_FOUND);
        }
        return toVO(dealer);
    }

    @Override
    public IPage<DealerVO> page(DealerQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<Dealer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getDealerName())) {
            wrapper.like(Dealer::getDealerName, query.getDealerName());
        }
        if (StringUtils.hasText(query.getDealerLevel())) {
            wrapper.eq(Dealer::getDealerLevel, query.getDealerLevel());
        }
        if (query.getParentId() != null) {
            wrapper.eq(Dealer::getParentId, query.getParentId());
        }
        if (StringUtils.hasText(query.getOrderBy())) {
            wrapper.last("ORDER BY " + query.getOrderBy() + " " + query.getOrderDirection());
        } else {
            wrapper.orderByDesc(Dealer::getCreatedAt);
        }
        Page<Dealer> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Dealer> entityPage = dealerMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public Dealer getByDealerCode(String dealerCode) {
        Dealer dealer = dealerMapper.selectByDealerCode(dealerCode);
        if (dealer == null) {
            throw new BusinessException(ResultCode.DEALER_NOT_FOUND);
        }
        return dealer;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Dealer dealer = dealerMapper.selectById(id);
        if (dealer == null) {
            throw new BusinessException(ResultCode.DEALER_NOT_FOUND);
        }
        dealerMapper.deleteById(id);
    }

    @Override
    public List<DealerOptionVO> options() {
        List<Dealer> dealers = dealerMapper.selectList(new LambdaQueryWrapper<Dealer>()
                .eq(Dealer::getStatus, "ACTIVE")
                .orderByAsc(Dealer::getDealerName));
        return dealers.stream().map(d -> {
            DealerOptionVO vo = new DealerOptionVO();
            vo.setId(d.getId());
            vo.setDealerName(d.getDealerName());
            vo.setDealerCode(d.getDealerCode());
            return vo;
        }).collect(Collectors.toList());
    }

    private DealerVO toVO(Dealer dealer) {
        DealerVO vo = new DealerVO();
        BeanUtils.copyProperties(dealer, vo);
        vo.setContactPerson(dealer.getContactName());
        vo.setContactPhone(dealer.getPhone());
        vo.setAddress(dealer.getRegion());
        vo.setLevel(parseLevel(dealer.getDealerLevel()));
        return vo;
    }

    private String convertLevel(Integer level) {
        if (level == null) {
            return DealerLevel.L1_DEALER.name();
        }
        return switch (level) {
            case 2 -> DealerLevel.L2_DEALER.name();
            case 3 -> DealerLevel.L3_DEALER.name();
            default -> DealerLevel.L1_DEALER.name();
        };
    }

    private Integer parseLevel(String dealerLevel) {
        if (dealerLevel == null) {
            return 1;
        }
        return switch (dealerLevel) {
            case "L2_DEALER" -> 2;
            case "L3_DEALER" -> 3;
            default -> 1;
        };
    }

    private String defaultPassword(DealerCreateDTO dto) {
        String raw = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : dto.getPhone();
        if (StringUtils.hasText(raw) && raw.length() >= 6) {
            return raw.substring(raw.length() - 6);
        }
        return "123456";
    }
}
