package com.aya.learning.moneybook.solution;

import com.aya.learning.moneybook.Category;

/**
 * Day 3 参考答案：枚举反查。
 */
public final class CategoryAnswer {

    private CategoryAnswer() {
    }

    public static Category fromDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return Category.OTHER;
        }
        String trimmed = name.trim();
        for (Category category : Category.values()) {
            if (category.getDisplayName().equals(trimmed)) {
                return category;
            }
        }
        return Category.OTHER;
    }
}
