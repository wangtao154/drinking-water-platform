package com.platform.water.vo;

import lombok.Data;

@Data
public class WaterWechatPayVO {

    private WaterDispenseOrderVO order;

    private WechatPayParamsVO payParams;
}
