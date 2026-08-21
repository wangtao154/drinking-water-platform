package com.platform.ai.assistant.service;

import com.platform.ai.assistant.dto.AdminAssistantChatRequest;
import com.platform.ai.assistant.dto.AdminAssistantChatResponse;

public interface AdminAssistantService {

    AdminAssistantChatResponse chat(AdminAssistantChatRequest request);
}
