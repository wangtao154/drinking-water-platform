package com.platform.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("worker_wechat_binding")
public class WorkerWechatBinding extends BaseEntity {

    private Long workerId;

    private String miniOpenId;

    private String officialOpenId;

    private String unionId;

    private Integer subscribeStatus;

    private LocalDateTime boundAt;

    private LocalDateTime unboundAt;
}
