package com.platform.water.entity;

import lombok.Data;

@Data
public class DeviceLookup {

    private Long id;

    private String deviceId;

    private String sn;

    private Integer onlineStatus;

    private String lifecycleStatus;
}
