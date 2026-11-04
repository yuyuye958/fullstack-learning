package com.aya.learning.loganalyzer;

import java.util.List;
import java.util.Map;

/**
 * 日志分析器骨架（第 1 周只做内存版，第 2 周加文件 IO、大文件流式处理和 CSV 输出）。
 * <p>日志行格式：{@code 2026-11-03T10:00:00 INFO [order] 创建订单成功}</p>
 */
public final class LogAnalyzer {

    private LogAnalyzer() {
    }

    /**
     * 解析一行日志。
     * <p>规则：字段不足 3 段、级别无法识别、消息为空 → 返回 {@code null}（坏行不抛异常，
     * 真实日志里脏行太多了，一条脏行不应该让整个分析任务失败）。</p>
     */
    public static LogEntry parseLine(String line) {
        throw new UnsupportedOperationException("TODO Day5：按空格切三段，级别用 LogLevel.from");
    }

    /**
     * 统计各日志级别的条数；坏行跳过。没有出现的级别也要有 0。
     */
    public static Map<LogLevel, Long> countByLevel(List<String> lines) {
        throw new UnsupportedOperationException("TODO Day6：parse 后 groupingBy(level, counting())");
    }

    /**
     * 取出现次数最多的前 limit 条 ERROR 消息，次数降序、次数相同按字典序。
     */
    public static List<String> topErrorMessages(List<String> lines, int limit) {
        throw new UnsupportedOperationException("TODO Day6：filter(ERROR) → groupingBy → 排序 → limit");
    }
}
