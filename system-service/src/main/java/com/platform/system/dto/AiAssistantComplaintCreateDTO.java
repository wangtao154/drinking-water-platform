package com.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 后台 AI 助手投诉或举报提交参数。 */
@Data
public class AiAssistantComplaintCreateDTO {

    @NotBlank(message = "请选择问题类型")
    @Size(max = 32, message = "问题类型不合法")
    private String category;

    @Size(max = 64, message = "关联请求标识过长")
    private String requestId;

    @NotBlank(message = "请填写投诉或举报说明")
    @Size(max = 1000, message = "投诉或举报说明不能超过1000个字符")
    private String content;
}
