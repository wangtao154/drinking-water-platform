package com.platform.system.resource;

import java.util.List;

public record ResourceSnapshot(String sampledAt, String collectorStatus, Host host,
                               Docker docker, Redis redis, Jvm jvm, List<Point> history) {
    public record Collector(String sampledAt, Host host, Docker docker) {}
    public record Host(String name, String os, Integer cores, Double cpuPercent,
                       Long totalMemoryBytes, Long usedMemoryBytes, List<Disk> disks) {}
    public record Disk(String name, Long totalBytes, Long freeBytes) {}
    public record Docker(String status, List<Container> containers) {}
    public record Container(String name, String state, String health, Integer restarts,
                            Double cpuPercent, String memoryUsage, Double memoryPercent,
                            String networkIO, String blockIO, Integer pids) {}
    public record Redis(String status, Long usedBytes, Long maxBytes, Long rssBytes,
                        Long clients, Long blockedClients, Long keys, Double hitRate,
                        Long evictedKeys, Long opsPerSecond, Long uptimeSeconds, String version) {}
    public record Jvm(long heapUsedBytes, long heapMaxBytes, int threads, long uptimeSeconds) {}
    public record Point(String sampledAt, Double cpuPercent, Double memoryPercent, Long redisUsedBytes) {}
}
