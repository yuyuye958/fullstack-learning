package com.aya.learning.moneybook;

import com.aya.learning.moneybook.solution.InMemoryMoneyBookServiceSolution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 记账本的验收测试（针对参考答案实现）。
 * <p>你写完自己的 {@code InMemoryMoneyBookService} 后，把这里的实现类换掉，
 * 全绿就说明功能对齐。</p>
 */
class MoneyBookServiceTest {

    private MoneyBookService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryMoneyBookServiceSolution();
        service.addIncome(LocalDate.of(2026, 10, 8), Category.SALARY, new BigDecimal("18000"), "10 月工资");
        service.addExpense(LocalDate.of(2026, 10, 12), Category.FOOD, new BigDecimal("45.5"), "外卖");
        service.addIncome(LocalDate.of(2026, 11, 3), Category.SALARY, new BigDecimal("17600"), "11 月工资");
        service.addExpense(LocalDate.of(2026, 11, 4), Category.FOOD, new BigDecimal("38"), "午饭");
        service.addExpense(LocalDate.of(2026, 11, 4), Category.TRANSPORT, new BigDecimal("6"), "地铁");
    }

    @Test
    @DisplayName("新增流水：id 自增、金额四舍五入到分")
    void add_shouldAssignIdAndScale() {
        Transaction transaction = service.addExpense(
                LocalDate.of(2026, 11, 4), Category.SHOPPING, new BigDecimal("199.005"), "键盘垫");
        assertEquals(6, transaction.getId());
        assertEquals(0, new BigDecimal("199.01").compareTo(transaction.getAmount()));
    }

    @Test
    @DisplayName("金额 <= 0 直接拒绝")
    void add_shouldRejectNonPositiveAmount() {
        assertThrows(InvalidAmountException.class, () -> service.addExpense(
                LocalDate.of(2026, 11, 4), Category.FOOD, new BigDecimal("-3"), "错账"));
    }

    @Test
    @DisplayName("余额 = 收入合计 - 支出合计")
    void balance_shouldBeIncomeMinusExpense() {
        // 18000 + 17600 - 45.50 - 38 - 6 = 35510.50
        assertEquals(0, new BigDecimal("35510.50").compareTo(service.balance()));
    }

    @Test
    @DisplayName("按月筛选只返回该月流水")
    void findByMonth_shouldOnlyReturnThatMonth() {
        List<Transaction> november = service.findByMonth(YearMonth.of(2026, 11));
        assertEquals(3, november.size());
        assertTrue(november.stream()
                .allMatch(transaction -> YearMonth.from(transaction.getDate()).equals(YearMonth.of(2026, 11))));
    }

    @Test
    @DisplayName("按分类汇总支出")
    void totalByCategory_shouldGroupExpense() {
        Map<Category, BigDecimal> totals = service.totalByCategory(TransactionType.EXPENSE);
        assertEquals(0, new BigDecimal("83.50").compareTo(totals.get(Category.FOOD)));
        assertEquals(0, new BigDecimal("6.00").compareTo(totals.get(Category.TRANSPORT)));
        assertFalse(totals.containsKey(Category.SALARY), "收入分类不应该出现在支出汇总里");
    }

    @Test
    @DisplayName("按金额倒序")
    void sortedByAmountDesc_shouldOrderByAmount() {
        List<Transaction> sorted = service.sortedByAmountDesc();
        assertEquals(5, sorted.size());
        assertEquals(0, new BigDecimal("18000").compareTo(sorted.get(0).getAmount()));
        assertEquals(0, new BigDecimal("6").compareTo(sorted.get(4).getAmount()));
    }

    @Test
    @DisplayName("导出 CSV：表头 + 每条流水一行")
    void exportCsv_shouldWriteHeaderAndAllRows(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("moneybook.csv");
        service.exportCsv(csv);
        List<String> lines = Files.readAllLines(csv);
        assertEquals(6, lines.size());
        assertEquals("id,date,type,category,amount,note", lines.get(0));
        assertTrue(lines.get(1).startsWith("1,2026-10-08,INCOME,SALARY,18000.00,"));
    }

    @Test
    @DisplayName("findAll 返回快照，后续新增不影响已拿到的列表")
    void findAll_shouldReturnSnapshot() {
        List<Transaction> snapshot = service.findAll();
        service.addExpense(LocalDate.of(2026, 11, 5), Category.OTHER, new BigDecimal("1"), "测试");
        assertEquals(5, snapshot.size());
    }
}
