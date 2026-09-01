package com.platform.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Manual identity verification action for a backend account.
 */
@Data
public class AccountIdentityVerificationDTO {

    @NotNull(message = "请指定身份核验状态")
    private Boolean verified;
}
