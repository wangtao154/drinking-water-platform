package com.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** AI 助手投诉或举报处理参数。 */
@Data
public class AiAssistantComplaintHandleDTO {

    @NotBlank(message = "请选择处理状态")
    @Size(max = 24, message = "处理状态不合法")
    private String status;

    @Size(max = 1000, message = "处理答复不能超过1000个字符")
    private String handleReply;
}
