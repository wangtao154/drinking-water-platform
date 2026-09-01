package com.platform.ai.assistant.conversation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory multi-turn conversation history, isolated per user + conversationId.
 *
 * <p>Design choices:
 * <ul>
 *   <li>No Redis dependency — ai-service has no direct Redis client wiring, and an
 *       in-memory store is sufficient for a single-instance assistant that handles
 *       dozens of admin users, not public-scale traffic.</li>
 *   <li>TTL 30 minutes; each append refreshes expiry. A scheduled task removes expired sessions.</li>
 *   <li>Keeps the last {@link #MAX_TURNS} turns (a turn = one user + one assistant message)
 *       to bound prompt size and cost.</li>
 *   <li>History stores only the sanitized question and the model answer; never raw
 *       PII, tool results, or credentials.</li>
 * </ul>
 */
@Slf4j
@Component
public class ConversationHistoryStore {

    /** Number of past turns (user+assistant pairs) retained per conversation. */
    static final int MAX_TURNS = 6;

    /** Idle TTL in seconds; a conversation unused for this long is evicted. */
    static final long TTL_SECONDS = 1800;

    private record Conversation(Deque<ChatMessage> messages, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final ConcurrentHashMap<String, Conversation> conversations = new ConcurrentHashMap<>();

    /**
     * Returns the existing conversationId, or generates a new one. Never null.
     */
    public String resolveOrNew(String conversationId, Long userId) {
        String key = key(userId, conversationId);
        if (conversationId != null && !conversationId.isBlank()) {
            Conversation existing = conversations.get(key);
            if (existing != null && !existing.isExpired()) {
                return conversationId;
            }
            if (existing != null) {
                conversations.remove(key, existing);
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Reads the history (oldest first) for the given conversation. Empty list if none or expired.
     */
    public List<ChatMessage> read(Long userId, String conversationId) {
        String key = key(userId, conversationId);
        Conversation conversation = conversations.get(key);
        if (conversation == null) {
            return List.of();
        }
        if (conversation.isExpired()) {
            conversations.remove(key, conversation);
            return List.of();
        }
        return new ArrayList<>(conversation.messages);
    }

    /**
     * Appends a user+assistant turn and refreshes the TTL.
     * Trims to the latest {@link #MAX_TURNS} turns when exceeded.
     */
    public void appendTurn(Long userId, String conversationId, String userMessage, String assistantMessage) {
        String key = key(userId, conversationId);
        Instant newExpiry = Instant.now().plusSeconds(TTL_SECONDS);
        conversations.compute(key, (k, existing) -> {
            Deque<ChatMessage> messages = (existing == null || existing.isExpired())
                    ? new ConcurrentLinkedDeque<>()
                    : existing.messages;
            messages.addLast(ChatMessage.user(userMessage));
            messages.addLast(ChatMessage.assistant(assistantMessage));
            // Trim to MAX_TURNS * 2 entries (each turn = 2 messages)
            while (messages.size() > MAX_TURNS * 2) {
                messages.pollFirst();
            }
            return new Conversation(messages, newExpiry);
        });
    }

    /**
     * Clears a conversation (used when admin clicks "clear conversation" in the UI).
     */
    public void clear(Long userId, String conversationId) {
        conversations.remove(key(userId, conversationId));
    }

    /**
     * Periodic eviction of expired conversations so abandoned browser sessions do not
     * remain in memory until another request happens to touch the same conversation.
     */
    @Scheduled(fixedDelayString = "${ai.assistant.conversation-cleanup-interval-ms:300000}")
    public void evictExpired() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Conversation> entry : conversations.entrySet()) {
            if (entry.getValue().isExpired()) {
                toRemove.add(entry.getKey());
            }
        }
        for (String key : toRemove) {
            conversations.remove(key);
        }
        if (!toRemove.isEmpty()) {
            log.debug("[Assistant] Evicted {} expired conversations", toRemove.size());
        }
    }

    private static String key(Long userId, String conversationId) {
        return "u:" + userId + ":c:" + (conversationId == null ? "default" : conversationId);
    }
}
