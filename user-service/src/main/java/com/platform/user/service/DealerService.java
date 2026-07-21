package com.platform.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.user.dto.DealerCreateDTO;
import com.platform.user.dto.DealerQueryDTO;
import com.platform.user.dto.DealerUpdateDTO;
import com.platform.user.entity.Dealer;
import com.platform.user.vo.DealerOptionVO;
import com.platform.user.vo.DealerVO;

import java.util.List;

public interface DealerService {

    DealerVO create(DealerCreateDTO dto);

    DealerVO update(Long id, DealerUpdateDTO dto);

    DealerVO getById(Long id);

    IPage<DealerVO> page(DealerQueryDTO query);

    Dealer getByDealerCode(String dealerCode);

    void delete(Long id);

    /** 小程序选择器用：返回所有启用的经销商（id + 名称） */
    List<DealerOptionVO> options();
}
