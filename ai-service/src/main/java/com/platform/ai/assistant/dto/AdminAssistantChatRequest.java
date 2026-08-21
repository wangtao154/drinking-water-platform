package com.platform.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminAssistantChatRequest {

    @NotBlank(message = "请输入需要咨询的问题")
    @Size(max = 1000, message = "问题不能超过1000个字符")
    private String question;
}
