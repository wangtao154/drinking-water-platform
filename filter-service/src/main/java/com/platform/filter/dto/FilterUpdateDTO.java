package com.platform.filter.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FilterUpdateDTO {

    private Long filterModelId;

    private LocalDate productionDate;

    private String productionBatch;

    private String lifecycleStatus;

    private String currentDeviceId;

    private String remark;
}
