package com.aya.learning.loganalyzer.solution;

import com.aya.learning.loganalyzer.LogEntry;
import com.aya.learning.loganalyzer.LogLevel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 参考答案：内存版日志分析。
 * <p>第 2 周会在同样的接口上接文件 IO（{@code Files.lines} 流式读）与 CSV 输出。</p>
 */
public final class LogAnalyzerSolution {

    private LogAnalyzerSolution() {
    }

    public static LogEntry parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String trimmed = line.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace <= 0) {
            return null;
        }
        String timestamp = trimmed.substring(0, firstSpace);
        int secondSpace = trimmed.indexOf(' ', firstSpace + 1);
        String levelToken = secondSpace < 0
                ? trimmed.substring(firstSpace + 1)
                : trimmed.substring(firstSpace + 1, secondSpace);
        LogLevel level = LogLevel.from(levelToken);
        if (level == null) {
            return null;
        }
        String message = secondSpace < 0 ? "" : trimmed.substring(secondSpace + 1).trim();
        if (message.isEmpty()) {
            return null;
        }
        return new LogEntry(timestamp, level, message);
    }

    public static Map<LogLevel, Long> countByLevel(List<String> lines) {
        Map<LogLevel, Long> counts = new EnumMap<>(LogLevel.class);
        for (LogLevel level : LogLevel.values()) {
            counts.put(level, 0L);
        }
        lines.stream()
                .map(LogAnalyzerSolution::parseLine)
                .filter(Objects::nonNull)
                .forEach(entry -> counts.merge(entry.getLevel(), 1L, Long::sum));
        return counts;
    }

    public static List<String> topErrorMessages(List<String> lines, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Map<String, Long> errorCounts = lines.stream()
                .map(LogAnalyzerSolution::parseLine)
                .filter(Objects::nonNull)
                .filter(entry -> entry.getLevel() == LogLevel.ERROR)
                .collect(Collectors.groupingBy(LogEntry::getMessage, Collectors.counting()));

        return errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}
