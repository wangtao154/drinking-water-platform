package com.platform.ai.assistant.service.impl;

import com.platform.ai.config.AdminAssistantProperties;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory safeguard. It is intentionally isolated from core services.
 */
@Component
@RequiredArgsConstructor
public class AdminAssistantRateLimiter {

    private final AdminAssistantProperties properties;
    private final ConcurrentHashMap<Long, Deque<Long>> requestTimes = new ConcurrentHashMap<>();

    public void check(Long userId) {
        long now = Instant.now().getEpochSecond();
        long threshold = now - properties.getRateLimitWindowSeconds();
        Deque<Long> queue = requestTimes.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() <= threshold) {
                queue.removeFirst();
            }
            if (queue.size() >= properties.getMaxRequestsPerUser()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "AI 助手请求过于频繁，请稍后再试");
            }
            queue.addLast(now);
        }
    }
}
