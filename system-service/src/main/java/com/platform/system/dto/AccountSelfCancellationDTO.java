package com.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Backend account self-cancellation confirmation.
 */
@Data
public class AccountSelfCancellationDTO {

    @NotBlank(message = "请输入当前账户密码")
    private String password;

    @NotBlank(message = "请输入注销确认文字")
    private String confirmText;
}
