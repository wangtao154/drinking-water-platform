package com.platform.common.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器
 * 64 位 Long 型全局唯一 ID
 * 结构：1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号
 */
@Component
public class SnowflakeIdUtil {

    /** 起始时间戳 (2024-01-01 00:00:00) */
    private static final long START_TIMESTAMP = 1704067200000L;

    /** 机器ID位数 */
    private static final long MACHINE_ID_BITS = 10L;

    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 机器ID最大值 */
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);

    /** 序列号掩码 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 机器ID左移位数 */
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;

    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdUtil() {
        this.machineId = 1L; // 默认机器ID=1，生产环境可通过配置注入
    }

    public SnowflakeIdUtil(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException("机器ID超出范围: 0~" + MAX_MACHINE_ID);
        }
        this.machineId = machineId;
    }

    /**
     * 生成下一个ID（线程安全）
     */
    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();

        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成字符串形式的ID
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
