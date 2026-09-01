package com.platform.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminAssistantChatRequest {

    @NotBlank(message = "请输入需要咨询的问题")
    @Size(max = 1000, message = "问题不能超过1000个字符")
    private String question;

    /**
     * Optional conversation identifier. Omit on the first message; the server
     * generates one and returns it in the response. Pass it back on follow-up
     * messages so the assistant can resolve multi-turn references (那/它/刚才).
     */
    @Size(max = 64, message = "会话标识格式不正确")
    private String conversationId;
}
