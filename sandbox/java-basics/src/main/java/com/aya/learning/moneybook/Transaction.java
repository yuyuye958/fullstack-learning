package com.aya.learning.moneybook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 一条收支流水（POJO，对应后端的 Entity / DTO 这一层）。
 * <p>和 JS 对象最大的不同：字段是 {@code private final}，构造时就校验完，
 * 外部拿不到“半成品对象”。金额统一保留两位小数，方向由 {@link TransactionType} 表达。</p>
 */
public class Transaction {

    private final long id;
    private final LocalDate date;
    private final TransactionType type;
    private final Category category;
    private final BigDecimal amount;
    private final String note;

    public Transaction(long id, LocalDate date, TransactionType type, Category category, BigDecimal amount, String note) {
        this.id = id;
        this.date = Objects.requireNonNull(date, "date 不能为 null");
        this.type = Objects.requireNonNull(type, "type 不能为 null");
        this.category = Objects.requireNonNull(category, "category 不能为 null");
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("金额必须大于 0：" + amount);
        }
        // 金额一律保留两位小数，避免 199.005 这种脏数据进入后续统计
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.note = note == null ? "" : note;
    }

    public long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public TransactionType getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    /**
     * 收入为正、支出为负，余额直接累加即可。
     * <p>如果你在 JS 里习惯写 {@code amount * (type === 'INCOME' ? 1 : -1)}，思路一样，
     * 只是这里把规则收进对象，避免每个调用点都重复判断。</p>
     */
    public BigDecimal getSignedAmount() {
        return type == TransactionType.INCOME ? amount : amount.negate();
    }

    @Override
    public String toString() {
        return String.format("#%d %s %s %-4s %10s  %s",
                id,
                date,
                type.getDisplayName(),
                category.getDisplayName(),
                amount.toPlainString(),
                note);
    }
}
