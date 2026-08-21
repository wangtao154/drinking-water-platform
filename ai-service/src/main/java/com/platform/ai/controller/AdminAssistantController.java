package com.platform.ai.controller;

import com.platform.ai.assistant.dto.AdminAssistantChatRequest;
import com.platform.ai.assistant.dto.AdminAssistantChatResponse;
import com.platform.ai.assistant.service.AdminAssistantService;
import com.platform.common.auth.RequirePermission;
import com.platform.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlled assistant API. It uses a permission-filtered knowledge base and
 * fixed read-only data tools only.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/assistant")
@RequirePermission("AI_ASSISTANT_VIEW")
public class AdminAssistantController {

    private final AdminAssistantService adminAssistantService;

    @PostMapping("/chat")
    public R<AdminAssistantChatResponse> chat(@Valid @RequestBody AdminAssistantChatRequest request) {
        return R.ok(adminAssistantService.chat(request));
    }
}
