package com.platform.ai.assistant.conversation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationHistoryStoreTest {

    private final ConversationHistoryStore store = new ConversationHistoryStore();

    @Test
    void shouldClearOnlyTheCurrentUsersConversation() {
        String conversationId = "conversation-1";
        store.appendTurn(10L, conversationId, "问题A", "回答A");
        store.appendTurn(20L, conversationId, "问题B", "回答B");

        store.clear(10L, conversationId);

        assertTrue(store.read(10L, conversationId).isEmpty());
        assertEquals(2, store.read(20L, conversationId).size());
    }

    @Test
    void shouldTreatRepeatedClearAsSuccess() {
        store.clear(10L, "missing-conversation");
        store.clear(10L, "missing-conversation");

        assertTrue(store.read(10L, "missing-conversation").isEmpty());
    }
}
