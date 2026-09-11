package com.platform.ai.assistant.external;

import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/** Per-key local sliding-window limiter. It protects the API even when a key leaks. */
@Component
public class ExternalAssistantApiRateLimiter {

    private final ConcurrentHashMap<Long, Deque<Long>> requestTimes = new ConcurrentHashMap<>();

    public void check(Long apiKeyId, int maxPerMinute) {
        long now = Instant.now().getEpochSecond();
        long threshold = now - 60;
        Deque<Long> queue = requestTimes.computeIfAbsent(apiKeyId, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() <= threshold) {
                queue.removeFirst();
            }
            if (queue.size() >= maxPerMinute) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "API Key 请求过于频繁，请稍后再试");
            }
            queue.addLast(now);
        }
    }
}
