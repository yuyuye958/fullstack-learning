package com.aya.learning.moneybook.demo;

import com.aya.learning.moneybook.Category;
import com.aya.learning.moneybook.MoneyBookService;
import com.aya.learning.moneybook.Transaction;
import com.aya.learning.moneybook.TransactionType;
import com.aya.learning.moneybook.solution.InMemoryMoneyBookServiceSolution;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 可运行的命令行演示：用参考答案把记账本的完整流程跑一遍。
 * <p>你写完自己的 {@code InMemoryMoneyBookService} 后，把第 12 行的实现换掉，
 * 再跑一次，对比输出是否一致。</p>
 *
 * <pre>
 * mvn -q compile
 * java -cp target/classes com.aya.learning.moneybook.demo.MoneyBookDemo
 * </pre>
 */
public final class MoneyBookDemo {

    private MoneyBookDemo() {
    }

    public static void main(String[] args) throws IOException {
        MoneyBookService service = new InMemoryMoneyBookServiceSolution();

        service.addIncome(LocalDate.of(2026, 10, 8), Category.SALARY, new BigDecimal("18000.00"), "10 月工资");
        service.addExpense(LocalDate.of(2026, 10, 12), Category.FOOD, new BigDecimal("45.50"), "外卖");
        service.addIncome(LocalDate.of(2026, 11, 3), Category.SALARY, new BigDecimal("17600.00"), "11 月工资");
        service.addExpense(LocalDate.of(2026, 11, 3), Category.FOOD, new BigDecimal("38.00"), "午饭");
        service.addExpense(LocalDate.of(2026, 11, 4), Category.TRANSPORT, new BigDecimal("6.00"), "地铁");
        service.addExpense(LocalDate.of(2026, 11, 4), Category.SHOPPING, new BigDecimal("199.00"), "键盘垫");

        System.out.println("== 全部流水 ==");
        for (Transaction transaction : service.findAll()) {
            System.out.println(transaction);
        }

        System.out.println();
        System.out.println("== 2026-11 月度流水 ==");
        for (Transaction transaction : service.findByMonth(YearMonth.of(2026, 11))) {
            System.out.println(transaction);
        }

        System.out.println();
        System.out.println("== 支出按分类（全部数据） ==");
        service.totalByCategory(TransactionType.EXPENSE)
                .forEach((category, sum) ->
                        System.out.printf("%-4s %10s%n", category.getDisplayName(), sum.toPlainString()));

        System.out.println();
        System.out.printf("收入合计=%s  支出合计=%s  余额=%s%n",
                service.total(TransactionType.INCOME).toPlainString(),
                service.total(TransactionType.EXPENSE).toPlainString(),
                service.balance().toPlainString());

        System.out.println();
        System.out.println("== 金额从大到小 Top3 ==");
        service.sortedByAmountDesc().stream()
                .limit(3)
                .forEach(System.out::println);

        Path csv = Paths.get("target", "moneybook-demo.csv");
        service.exportCsv(csv);
        System.out.println();
        System.out.println("CSV 已导出: " + csv.toAbsolutePath()
                + "（" + Files.readAllLines(csv).size() + " 行，含表头）");
    }
}
