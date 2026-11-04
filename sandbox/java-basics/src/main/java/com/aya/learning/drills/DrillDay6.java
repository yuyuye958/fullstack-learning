package com.aya.learning.drills;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Day 6 练习：金额精度与日期时间。
 * <p>JS 用 Number 算钱也会遇到 0.1 + 0.2 的问题，Java 里更明显：{@code double} 是二进制浮点，
 * 金额必须用 {@link BigDecimal}，而且要从字符串构造。</p>
 */
public final class DrillDay6 {

    private DrillDay6() {
    }

    /**
     * 用 double 把 0.1 相加三次并返回。
     * <p>验收：结果不是 0.3——这就是不能用 double 算钱的原因。</p>
     */
    public static double doubleSumProblem() {
        throw new UnsupportedOperationException("TODO Day6-1");
    }

    /**
     * 用 BigDecimal 把 "0.1" 相加三次并返回。
     * <p>要求：不能用 {@code new BigDecimal(0.1)} 这种从 double 构造的写法。</p>
     */
    public static BigDecimal bigDecimalSumFixed() {
        throw new UnsupportedOperationException("TODO Day6-2");
    }

    /**
     * 把日期格式化成 {@code 2026年11月04日}。
     * <p>用 {@link java.time.format.DateTimeFormatter}，不要用 SimpleDateFormat（可变、非线程安全）。</p>
     */
    public static String formatDate(LocalDate date) {
        throw new UnsupportedOperationException("TODO Day6-3");
    }
}
