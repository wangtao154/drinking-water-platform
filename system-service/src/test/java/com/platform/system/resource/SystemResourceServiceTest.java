package com.platform.system.resource;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class SystemResourceServiceTest {
    @Test
    void missingAndUnlimitedMetricsAreNotZeroPercent() {
        Properties info = new Properties();
        info.setProperty("maxmemory", "0");
        info.setProperty("keyspace_hits", "0");
        info.setProperty("keyspace_misses", "0");
        var redis = SystemResourceService.redisMetrics(info);
        assertEquals(0L, redis.maxBytes());
        assertNull(redis.usedBytes());
        assertNull(redis.hitRate());
        assertNull(SystemResourceService.percent(10L, 0L));
    }

    @Test
    void aggregatesKeysAcrossDatabasesAndCalculatesHitRate() {
        Properties info = new Properties();
        info.setProperty("db0", "keys=12,expires=3,avg_ttl=10");
        info.setProperty("db1", "keys=8,expires=0,avg_ttl=0");
        info.setProperty("keyspace_hits", "30");
        info.setProperty("keyspace_misses", "10");
        var redis = SystemResourceService.redisMetrics(info);
        assertEquals(20L, redis.keys());
        assertEquals(75.0, redis.hitRate());
    }

    @Test
    void rejectsExpiredInvalidAndFutureSamples() {
        Instant now = Instant.parse("2026-09-07T08:00:00Z");
        assertTrue(SystemResourceService.isFresh(now.minusSeconds(20).toString(), now));
        assertFalse(SystemResourceService.isFresh(now.minusSeconds(46).toString(), now));
        assertFalse(SystemResourceService.isFresh(now.plusSeconds(60).toString(), now));
        assertFalse(SystemResourceService.isFresh("invalid", now));
    }
}
