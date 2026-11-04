package com.aya.learning.loganalyzer.demo;

import com.aya.learning.loganalyzer.LogLevel;
import com.aya.learning.loganalyzer.solution.LogAnalyzerSolution;

import java.util.List;
import java.util.Map;

/**
 * 可运行的命令行演示：对一段内存日志做级别统计和 Top N 错误。
 *
 * <pre>
 * mvn -q compile
 * java -cp target/classes com.aya.learning.loganalyzer.demo.LogAnalyzerDemo
 * </pre>
 */
public final class LogAnalyzerDemo {

    private LogAnalyzerDemo() {
    }

    private static final List<String> SAMPLE_LINES = List.of(
            "2026-11-03T10:00:00 INFO [order] 创建订单成功",
            "2026-11-03T10:00:01 INFO [order] 查询订单列表",
            "2026-11-03T10:00:02 WARN [payment] 支付回调重试 1/3",
            "2026-11-03T10:00:03 ERROR [payment] 支付回调签名校验失败",
            "2026-11-03T10:00:04 ERROR [payment] 支付回调签名校验失败",
            "2026-11-03T10:00:05 ERROR [order] 订单状态非法: CANCELLED -> PAID",
            "garbage line without timestamp",
            "2026-11-03T10:00:06 TRACE [cache] 缓存命中"
    );

    public static void main(String[] args) {
        System.out.println("== 原始行数 " + SAMPLE_LINES.size() + " 条 ==");
        System.out.println();

        System.out.println("== 级别统计（坏行已跳过） ==");
        Map<LogLevel, Long> counts = LogAnalyzerSolution.countByLevel(SAMPLE_LINES);
        counts.forEach((level, count) -> System.out.printf("%-5s %d%n", level, count));

        System.out.println();
        System.out.println("== Top 2 ERROR 消息 ==");
        List<String> topErrors = LogAnalyzerSolution.topErrorMessages(SAMPLE_LINES, 2);
        for (int i = 0; i < topErrors.size(); i++) {
            System.out.printf("%d) %s%n", i + 1, topErrors.get(i));
        }
    }
}
