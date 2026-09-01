package com.platform.ai.assistant.conversation;

/**
 * A single message in a multi-turn conversation.
 *
 * @param role    "user" or "assistant"
 * @param content message content (already sanitized for external model when role=user)
 */
public record ChatMessage(String role, String content) {

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
