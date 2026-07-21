package com.platform.user.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OfficialAccountBindStatusVO {

    private Boolean bound;

    private Integer subscribeStatus;

    private LocalDateTime boundAt;

    private String officialOpenIdMasked;
}
