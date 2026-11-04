package com.aya.learning.moneybook;

/**
 * 流水方向：收入还是支出。
 * <p>金额字段永远存正数，方向单独存，统计时按需取符号——比“收入正数、支出负数”更好排查。</p>
 */
public enum TransactionType {

    INCOME("收入"),
    EXPENSE("支出");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
