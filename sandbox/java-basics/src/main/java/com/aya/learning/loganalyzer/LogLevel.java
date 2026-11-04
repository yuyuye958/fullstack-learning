package com.aya.learning.loganalyzer;

import java.util.Locale;

/**
 * 日志级别。
 * <p>真实项目里常见的是枚举 + {@code valueOf}；解析外部输入时要把“不认识”当成坏数据，
 * 而不是让 {@code IllegalArgumentException} 冒到上层。</p>
 */
public enum LogLevel {

    DEBUG,
    INFO,
    WARN,
    ERROR;

    /**
     * 把 "INFO" / "info" / " INFO " 统一解析成枚举，无法识别返回 null。
     */
    public static LogLevel from(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return valueOf(token.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
