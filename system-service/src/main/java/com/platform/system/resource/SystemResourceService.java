package com.platform.system.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Properties;
import static com.platform.system.resource.ResourceSnapshot.*;

@Slf4j
@Service
@EnableScheduling
public class SystemResourceService {
    private final RedisConnectionFactory factory;
    private final ObjectMapper mapper;
    private final Path snapshotPath;
    private final ArrayDeque<Point> history = new ArrayDeque<>();
    private volatile ResourceSnapshot latest;

    public SystemResourceService(RedisConnectionFactory factory, ObjectMapper mapper,
            @Value("${system.resources.snapshot-path:/app/resource-monitor/snapshot.json}") String path) {
        this.factory = factory;
        this.mapper = mapper;
        this.snapshotPath = Path.of(path);
        this.latest = new ResourceSnapshot(null, "UNAVAILABLE", null, null, unavailableRedis(), jvm(), List.of());
    }

    public ResourceSnapshot snapshot() { return latest; }

    @Scheduled(fixedDelay = 10000)
    public void sample() {
        Instant now = Instant.now();
        Collector collector = null;
        String status = "UNAVAILABLE";
        try {
            if (Files.isRegularFile(snapshotPath) && Files.size(snapshotPath) <= 1_048_576) {
                collector = mapper.readValue(Files.readAllBytes(snapshotPath), Collector.class);
                status = isFresh(collector.sampledAt(), now) ? "OK" : "STALE";
            }
        } catch (Exception ex) { log.debug("Resource snapshot unavailable: {}", ex.getClass().getSimpleName()); }
        Redis redis = readRedis();
        Host host = collector == null ? null : collector.host();
        // Expired samples are gaps in live trends, not fresh zero readings.
        boolean fresh = "OK".equals(status);
        history.addLast(new Point(now.toString(), fresh && host != null ? host.cpuPercent() : null,
                fresh && host != null ? percent(host.usedMemoryBytes(), host.totalMemoryBytes()) : null, redis.usedBytes()));
        while (!history.isEmpty() && Instant.parse(history.getFirst().sampledAt()).isBefore(now.minusSeconds(1800))) history.removeFirst();
        latest = new ResourceSnapshot(collector == null ? null : collector.sampledAt(), status,
                host, collector == null ? null : collector.docker(), redis, jvm(), List.copyOf(history));
    }

    static boolean isFresh(String timestamp, Instant now) {
        try {
            long age = Duration.between(Instant.parse(timestamp), now).getSeconds();
            return age >= -5 && age <= 45;
        } catch (Exception ex) { return false; }
    }

    private Redis readRedis() {
        try (RedisConnection connection = factory.getConnection()) {
            return redisMetrics(connection.serverCommands().info());
        } catch (Exception ex) {
            log.debug("Redis resource metrics unavailable: {}", ex.getClass().getSimpleName());
            return unavailableRedis();
        }
    }

    static Redis redisMetrics(Properties info) {
        if (info == null) return unavailableRedis();
        Long hits = number(info, "keyspace_hits"), misses = number(info, "keyspace_misses");
        Long keys = 0L;
        for (String name : info.stringPropertyNames()) {
            if (name.matches("db\\d+")) {
                for (String part : info.getProperty(name).split(",")) {
                    if (part.startsWith("keys=")) keys += Long.parseLong(part.substring(5));
                }
            }
        }
        return new Redis("OK", number(info, "used_memory"), number(info, "maxmemory"),
                number(info, "used_memory_rss"), number(info, "connected_clients"), number(info, "blocked_clients"),
                keys, hits == null || misses == null ? null : percent(hits, hits + misses),
                number(info, "evicted_keys"), number(info, "instantaneous_ops_per_sec"),
                number(info, "uptime_in_seconds"), info.getProperty("redis_version"));
    }

    private static Long number(Properties info, String key) {
        try { return Long.valueOf(info.getProperty(key)); } catch (Exception ex) { return null; }
    }

    static Double percent(Long used, Long total) {
        return used == null || total == null || total <= 0 ? null : used * 100.0 / total;
    }

    private static Redis unavailableRedis() {
        return new Redis("UNAVAILABLE", null, null, null, null, null, null, null, null, null, null, null);
    }

    private static Jvm jvm() {
        var memory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return new Jvm(memory.getUsed(), memory.getMax(), ManagementFactory.getThreadMXBean().getThreadCount(),
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
    }
}
