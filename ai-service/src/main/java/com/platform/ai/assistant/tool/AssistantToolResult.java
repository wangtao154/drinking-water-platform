package com.platform.ai.assistant.tool;

import com.platform.ai.assistant.dto.AdminAssistantDataSourceVO;

/** Internal execution result. */
public record AssistantToolResult(AdminAssistantDataSourceVO source, String modelContext) {
}
