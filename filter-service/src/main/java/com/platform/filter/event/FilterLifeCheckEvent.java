package com.platform.filter.event;

import lombok.Data;

import java.io.Serializable;

@Data
public class FilterLifeCheckEvent implements Serializable {

    private String deviceId;

    private String sn;
}
