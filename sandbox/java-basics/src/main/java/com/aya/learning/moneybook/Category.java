package com.aya.learning.moneybook;

/**
 * 收支分类。
 * <p>为什么用枚举而不是字符串常量：编译器能帮你挡住拼写错误，switch 能穷举，
 * 以后加字段（图标、预算上限）也只改一个地方。前端里常见的 {@code const CATEGORY = {...}}
 * 在 Java 里通常就落成枚举。</p>
 */
public enum Category {

    FOOD("餐饮"),
    TRANSPORT("交通"),
    SHOPPING("购物"),
    SALARY("工资"),
    OTHER("其他");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Day 3 练习：按中文名反查枚举，找不到返回 {@link #OTHER}。
     * <p>骨架未实现，先留给你写。</p>
     */
    public static Category fromDisplayName(String name) {
        throw new UnsupportedOperationException("TODO Day3：遍历 values() 比较 displayName");
    }
}
