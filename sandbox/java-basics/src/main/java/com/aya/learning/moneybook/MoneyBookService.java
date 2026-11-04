package com.aya.learning.moneybook;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 记账本的核心能力（Service 接口）。
 * <p>面向接口编程的意义：CLI 或 Controller 只依赖这个接口，
 * 以后把内存存储换成 MySQL，只换实现类，调用方一行都不用改。</p>
 */
public interface MoneyBookService {

    /**
     * 新增一条流水，返回带 id 的完整对象。
     *
     * @throws InvalidAmountException 金额为空、非数字或小于等于 0
     */
    Transaction add(LocalDate date, TransactionType type, Category category, BigDecimal amount, String note);

    /**
     * 记一笔收入。接口的 default 方法：调用方少传一个参数，实现类也不用重复写两遍。
     */
    default Transaction addIncome(LocalDate date, Category category, BigDecimal amount, String note) {
        return add(date, TransactionType.INCOME, category, amount, note);
    }

    /**
     * 记一笔支出。
     */
    default Transaction addExpense(LocalDate date, Category category, BigDecimal amount, String note) {
        return add(date, TransactionType.EXPENSE, category, amount, note);
    }

    /**
     * 全部流水（按插入顺序），返回的是快照，外部改不影响内部。
     */
    List<Transaction> findAll();

    /**
     * 某个月份的流水。
     */
    List<Transaction> findByMonth(YearMonth month);

    /**
     * 按分类汇总某一方向的金额（结果按分类名稳定排序）。
     */
    Map<Category, BigDecimal> totalByCategory(TransactionType type);

    /**
     * 某一方向的总金额（收入合计或支出合计）。
     */
    BigDecimal total(TransactionType type);

    /**
     * 余额 = 收入合计 - 支出合计。
     */
    BigDecimal balance();

    /**
     * 按金额从大到小排序（不分方向）。
     */
    List<Transaction> sortedByAmountDesc();

    /**
     * 导出 CSV（UTF-8，含表头）。
     * <p>这里 {@code IOException} 是受检异常：文件写不了是调用方必须面对的问题，
     * 编译器强制你处理，和 JS 的 Promise reject 完全不同。</p>
     */
    void exportCsv(Path path) throws IOException;
}
