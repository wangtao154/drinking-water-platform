package com.platform.filter.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FilterModelVO {

    private Long id;

    private String modelCode;

    private String modelName;

    private String category;

    private Integer filterLevel;

    private Integer standardLifeDuration;

    private Long standardLifeFlow;

    private Long price;

    private String photoUrl;

    private String description;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
