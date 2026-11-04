package com.aya.learning.drills;

import com.aya.learning.moneybook.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Day 4 练习：异常、自定义异常、异常转换。
 * <p>JS 里用 {@code throw new Error(...)} + {@code Promise.reject}；Java 里异常分两类：
 * 受检（编译器强制处理，如 IOException）和非受检（RuntimeException 的子类，编译期不强制）。
 * 业务校验失败通常用非受检异常 + 统一异常处理器换成 HTTP 状态码。</p>
 */
public final class DrillDay4 {

    private DrillDay4() {
    }

    /**
     * 把用户输入的金额字符串转成 BigDecimal。
     * <p>规则：</p>
     * <ol>
     *   <li>null、空白、不是数字 → 抛 {@link InvalidAmountException}，消息写清原因；</li>
     *   <li>小于等于 0 → 抛 {@link InvalidAmountException}；</li>
     *   <li>合法值保留两位小数（HALF_UP）。</li>
     * </ol>
     * <p>不要用 {@code Double.parseDouble} 再转 BigDecimal——精度已经丢了，
     * 必须 {@code new BigDecimal(String)}。</p>
     */
    public static BigDecimal parseAmount(String raw) {
        throw new UnsupportedOperationException("TODO Day4-1");
    }

    /**
     * 保留一个示例方法，说明 RoundingMode 的用法（可以直接读，不用改）。
     */
    public static BigDecimal roundToCent(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
