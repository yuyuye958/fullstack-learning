package com.aya.learning.loganalyzer;

import com.aya.learning.loganalyzer.solution.LogAnalyzerSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 日志分析器的验收测试（针对参考答案实现）。
 */
class LogAnalyzerTest {

    private static final List<String> LINES = List.of(
            "2026-11-03T10:00:00 INFO [order] 创建订单成功",
            "2026-11-03T10:00:01 WARN [payment] 支付回调重试 1/3",
            "2026-11-03T10:00:02 ERROR [payment] 支付回调签名校验失败",
            "2026-11-03T10:00:03 ERROR [payment] 支付回调签名校验失败",
            "2026-11-03T10:00:04 ERROR [order] 订单状态非法: CANCELLED -> PAID",
            "this is not a log line",
            ""
    );

    @Test
    @DisplayName("正常行能解析出时间、级别、消息")
    void parseLine_valid() {
        LogEntry entry = LogAnalyzerSolution.parseLine("2026-11-03T10:00:00 INFO [order] 创建订单成功");
        assertNotNull(entry);
        assertEquals(LogLevel.INFO, entry.getLevel());
        assertEquals("2026-11-03T10:00:00", entry.getTimestamp());
        assertEquals("[order] 创建订单成功", entry.getMessage());
    }

    @Test
    @DisplayName("坏行返回 null，不抛异常")
    void parseLine_badLineReturnsNull() {
        assertNull(LogAnalyzerSolution.parseLine("garbage"));
        assertNull(LogAnalyzerSolution.parseLine(""));
        assertNull(LogAnalyzerSolution.parseLine(null));
        assertNull(LogAnalyzerSolution.parseLine("2026-11-03T10:00:00 TRACE 缓存命中"));
    }

    @Test
    @DisplayName("级别统计包含 0 值，坏行跳过")
    void countByLevel() {
        Map<LogLevel, Long> counts = LogAnalyzerSolution.countByLevel(LINES);
        assertEquals(0L, counts.get(LogLevel.DEBUG));
        assertEquals(1L, counts.get(LogLevel.INFO));
        assertEquals(1L, counts.get(LogLevel.WARN));
        assertEquals(3L, counts.get(LogLevel.ERROR));
    }

    @Test
    @DisplayName("Top N 错误按次数降序")
    void topErrorMessages() {
        List<String> top = LogAnalyzerSolution.topErrorMessages(LINES, 2);
        assertEquals(2, top.size());
        assertEquals("[payment] 支付回调签名校验失败", top.get(0));
        assertEquals("[order] 订单状态非法: CANCELLED -> PAID", top.get(1));
    }
}
