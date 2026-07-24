package com.platform.water.service;

import com.platform.water.dto.Q74PreviewDTO;
import com.platform.water.dto.WaterDispenseCreateDTO;
import com.platform.water.vo.Q74ProtocolVO;
import com.platform.water.vo.WaterDispenseOrderVO;

public interface WaterDispenseService {

    WaterDispenseOrderVO createOrder(WaterDispenseCreateDTO dto);

    WaterDispenseOrderVO mockPayAndDispatch(String orderNo);

    WaterDispenseOrderVO getByOrderNo(String orderNo);

    Q74ProtocolVO previewQ74(Q74PreviewDTO dto);
}
