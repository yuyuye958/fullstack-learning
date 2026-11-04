package com.aya.learning.moneybook;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内存版实现骨架：第 1 周每天补一块，最后能跑通 {@code MoneyBookDemo} 的全部输出。
 * <p>字段已经给好，方法体全部是 TODO。写完后把 {@code MoneyBookDemo} 里的
 * {@code InMemoryMoneyBookServiceSolution} 换成 {@code new InMemoryMoneyBookService()}，
 * 再跑 {@code mvn test}（记得把测试里的实现类也换掉）对比结果。</p>
 */
public class InMemoryMoneyBookService implements MoneyBookService {

    /** 内存里的流水表，等价于 MySQL 里的一张表。 */
    private final List<Transaction> transactions = new ArrayList<>();

    /** 自增主键，等价于数据库的 auto_increment。 */
    private long sequence = 0;

    @Override
    public Transaction add(LocalDate date, TransactionType type, Category category, BigDecimal amount, String note) {
        // TODO Day2：++sequence 做 id；new Transaction(...)；add 进列表；return
        throw new UnsupportedOperationException("TODO Day2：新增流水");
    }

    @Override
    public List<Transaction> findAll() {
        // TODO Day5：返回 List.copyOf(transactions)，不要让外部直接拿到内部列表
        throw new UnsupportedOperationException("TODO Day5：返回快照");
    }

    @Override
    public List<Transaction> findByMonth(YearMonth month) {
        // TODO Day5：先用手写 for 循环 + YearMonth.from(t.getDate()).equals(month)
        throw new UnsupportedOperationException("TODO Day5：按月筛选");
    }

    @Override
    public Map<Category, BigDecimal> totalByCategory(TransactionType type) {
        // TODO Day5：Map<Category, BigDecimal>，遍历时 merge(category, amount, BigDecimal::add)
        throw new UnsupportedOperationException("TODO Day5：按分类汇总");
    }

    @Override
    public BigDecimal total(TransactionType type) {
        // TODO Day5：累加指定方向的 amount
        throw new UnsupportedOperationException("TODO Day5：方向合计");
    }

    @Override
    public BigDecimal balance() {
        // TODO Day6：累加 getSignedAmount()，用 BigDecimal.ZERO 作为起点
        throw new UnsupportedOperationException("TODO Day6：余额");
    }

    @Override
    public List<Transaction> sortedByAmountDesc() {
        // TODO Day5/6：new ArrayList<>(transactions)，再 sort(Comparator...)
        throw new UnsupportedOperationException("TODO Day5：按金额倒序");
    }

    @Override
    public void exportCsv(Path path) throws IOException {
        // TODO Day7：Files.createDirectories(path.getParent()) + Files.newBufferedWriter(UTF-8)
        //            写表头 id,date,type,category,amount,note，再逐行写；用 try-with-resources
        throw new UnsupportedOperationException("TODO Day7：导出 CSV");
    }
}
