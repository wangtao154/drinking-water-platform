package com.platform.filter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("filter_model")
public class FilterModel extends BaseEntity {

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
}
