package com.aya.learning.moneybook.solution;

import com.aya.learning.moneybook.Category;
import com.aya.learning.moneybook.MoneyBookService;
import com.aya.learning.moneybook.Transaction;
import com.aya.learning.moneybook.TransactionType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 参考答案：内存版记账本。
 * <p>先自己写 {@code com.aya.learning.moneybook.InMemoryMoneyBookService}，
 * 再对照这里。实现思路都一样，差别只在写法。</p>
 */
public class InMemoryMoneyBookServiceSolution implements MoneyBookService {

    private final List<Transaction> transactions = new ArrayList<>();
    private long sequence = 0;

    @Override
    public Transaction add(LocalDate date, TransactionType type, Category category, BigDecimal amount, String note) {
        long id = ++sequence;
        Transaction transaction = new Transaction(id, date, type, category, amount, note);
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findAll() {
        return List.copyOf(transactions);
    }

    @Override
    public List<Transaction> findByMonth(YearMonth month) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (YearMonth.from(transaction.getDate()).equals(month)) {
                result.add(transaction);
            }
        }
        return result;
    }

    @Override
    public Map<Category, BigDecimal> totalByCategory(TransactionType type) {
        // TreeMap 保证输出顺序稳定（按枚举定义顺序），测试和 CLI 输出才不会随机
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
    }

    @Override
    public BigDecimal total(TransactionType type) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getType() == type) {
                sum = sum.add(transaction.getAmount());
            }
        }
        return sum;
    }

    @Override
    public BigDecimal balance() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            sum = sum.add(transaction.getSignedAmount());
        }
        return sum;
    }

    @Override
    public List<Transaction> sortedByAmountDesc() {
        List<Transaction> copy = new ArrayList<>(transactions);
        copy.sort(Comparator.comparing(Transaction::getAmount).reversed());
        return copy;
    }

    @Override
    public void exportCsv(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("id,date,type,category,amount,note");
            writer.newLine();
            for (Transaction transaction : transactions) {
                writer.write(toCsvLine(transaction));
                writer.newLine();
            }
        }
    }

    private String toCsvLine(Transaction transaction) {
        return String.join(",",
                String.valueOf(transaction.getId()),
                transaction.getDate().toString(),
                transaction.getType().name(),
                transaction.getCategory().name(),
                transaction.getAmount().toPlainString(),
                escape(transaction.getNote()));
    }

    /** note 里可能有逗号或引号，按 CSV 规则用双引号包裹并转义。 */
    private String escape(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
