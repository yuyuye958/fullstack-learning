package com.aya.learning.loganalyzer;

import java.util.Objects;

/**
 * 一条解析后的日志（POJO）。
 * <p>Week 2 会在此基础上加字段（线程名、traceId）并改为从文件流式读取。</p>
 */
public class LogEntry {

    private final String timestamp;
    private final LogLevel level;
    private final String message;

    public LogEntry(String timestamp, LogLevel level, String message) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp 不能为 null");
        this.level = Objects.requireNonNull(level, "level 不能为 null");
        this.message = message == null ? "" : message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return timestamp + " " + level + " " + message;
    }
}
