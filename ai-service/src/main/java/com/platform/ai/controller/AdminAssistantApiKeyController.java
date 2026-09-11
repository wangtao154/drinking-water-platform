package com.platform.ai.controller;

import com.platform.ai.assistant.dto.AdminAssistantApiKeyCreateRequest;
import com.platform.ai.assistant.dto.AdminAssistantApiKeyCreateResponse;
import com.platform.ai.assistant.dto.AdminAssistantApiKeyStatusRequest;
import com.platform.ai.assistant.dto.AdminAssistantApiKeyVO;
import com.platform.ai.assistant.external.AdminAssistantApiKeyService;
import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.RequirePermission;
import com.platform.common.auth.UserContext;
import com.platform.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administrator-only lifecycle API for external read-only assistant credentials. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/assistant/api-keys")
@RequirePermission("AI_ASSISTANT_API_KEY_MANAGE")
public class AdminAssistantApiKeyController {

    private final AdminAssistantApiKeyService apiKeyService;

    @PostMapping
    public R<AdminAssistantApiKeyCreateResponse> create(@Valid @RequestBody AdminAssistantApiKeyCreateRequest request) {
        return R.ok(apiKeyService.create(request, currentUser()));
    }

    @GetMapping
    public R<List<AdminAssistantApiKeyVO>> list() {
        return R.ok(apiKeyService.list(currentUser()));
    }

    @PatchMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody AdminAssistantApiKeyStatusRequest request) {
        apiKeyService.changeStatus(id, request.getStatus(), currentUser());
        return R.ok(null);
    }

    private CurrentUser currentUser() {
        return UserContext.get();
    }
}
