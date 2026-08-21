package com.platform.ai.assistant.knowledge;

import java.util.List;

public record AdminKnowledgeDocument(
        String id,
        String title,
        String scope,
        List<String> permissions,
        List<String> sources,
        String updatedAt,
        String content
) {
}
