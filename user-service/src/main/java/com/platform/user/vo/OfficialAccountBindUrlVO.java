package com.platform.user.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OfficialAccountBindUrlVO {

    private Boolean bound;

    private String bindUrl;

    private Boolean mock;

    private Integer expiresInSeconds;
}
