package com.aya.learning.drills;

/**
 * Day 1 练习：基本类型、字符串、== 与 equals。
 * <p>JS 直觉：两个字符串用 {@code ===} 比内容。Java 的 {@code ==} 比的是“是不是同一个对象”，
 * 比较内容必须用 {@code equals}。</p>
 *
 * <p>做完后把 {@code DrillsTest} 里的 {@code Answers.xxx} 换成 {@code DrillDay1.xxx} 验证。</p>
 */
public final class DrillDay1 {

    private DrillDay1() {
    }

    /**
     * 用 {@code ==} 比较两个字符串。
     * <p>验收：传两个相同的字面量应为 true；传 {@code new String("a")} 和 {@code "a"} 应为 false。</p>
     */
    public static boolean sameWithOperator(String left, String right) {
        throw new UnsupportedOperationException("TODO Day1-1：用 == 实现");
    }

    /**
     * 用 {@code equals} 比较两个字符串。
     * <p>注意 left 可能为 null，直接用 {@code left.equals(right)} 会抛 NPE，用 {@code Objects.equals}。</p>
     */
    public static boolean sameWithEquals(String left, String right) {
        throw new UnsupportedOperationException("TODO Day1-2：用 Objects.equals 实现");
    }

    /**
     * 求两个 int 的平均值，要求 {@code averageOfTwo(5, 2)} 得到 3.5。
     * <p>这是 JS 与 Java 差异最大的一处：写成 {@code (a + b) / 2} 会先做整数除法（7/2=3），
     * 再自动转成 double 也是 3.0，不是 3.5。</p>
     */
    public static double averageOfTwo(int a, int b) {
        throw new UnsupportedOperationException("TODO Day1-3：注意先转 double 再除");
    }

    /**
     * 用 {@code ==} 比较两个装箱后的 int。
     * <p>验收：127 与 127 为 true；1000 与 1000 为 false（Integer 缓存只覆盖 -128~127）。</p>
     */
    public static boolean boxedSame(int a, int b) {
        throw new UnsupportedOperationException("TODO Day1-4：用 Integer.valueOf 装箱后比较");
    }
}
