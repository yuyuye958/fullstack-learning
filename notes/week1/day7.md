# Day 7：文件 IO、CSV、JUnit 5 与本周验收

> 时长 5h：上午 2h IO + 测试，下午 2.5h 收尾 + 验收，晚上 0.5h 自测与复盘

---

## 1. 文件 IO 最小够用集

Java 的 IO 类很多，本周只学 `java.nio.file` 这一套（JDK 7+），够你写导出、读配置、分析日志：

```java
Path path = Paths.get("target", "moneybook-demo.csv");   // 路径，不读不写
Path absolute = path.toAbsolutePath();                    // 绝对路径
Files.exists(path);                                       // 是否存在
Files.createDirectories(path.getParent());                // 建目录（父目录可以为 null，要先判断）

Files.readString(path, StandardCharsets.UTF_8);            // 整个文件读成字符串（小文件）
List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);  // 按行读
Files.writeString(path, "hello", StandardCharsets.UTF_8); // 整个写入（覆盖）

try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
    writer.write("id,date");   // 逐行写，适合大文件
    writer.newLine();
}
```

三个必须记住的点：

1. **显式指定 `StandardCharsets.UTF_8`**。不指定就用平台默认编码，Windows 中文环境下可能写出 GBK，别人打开是乱码。
2. **用 try-with-resources**。`writer` 不关会丢缓冲区里的数据。
3. **相对路径的基准是「启动 JVM 的当前目录」**。你在 `java-basics` 目录下跑，`target/...` 就是对的；在 IDEA 里跑，默认也是模块目录，但可以用绝对路径避免踩坑。

**异常**：文件不存在抛 `NoSuchFileException`（`IOException` 的子类，**受检**）；没有权限抛 `AccessDeniedException`。这些都必须处理或 `throws`。

---

## 2. CSV：格式简单，坑不少

规则只有三条：

1. 每行一条记录，字段用逗号分隔；
2. 字段里**有逗号、引号或换行**时，整个字段用双引号包起来；
3. 字段里的双引号写成两个双引号（`"` → `""`）。

```java
private String escape(String value) {
    if (value.contains(",") || value.contains("\"")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}
```

`exportCsv` 的完整实现（参考答案里就是这段）：

```java
@Override
public void exportCsv(Path path) throws IOException {
    if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
    }
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
        writer.write("id,date,type,category,amount,note");
        writer.newLine();
        for (Transaction transaction : transactions) {
            writer.write(String.join(",",
                    String.valueOf(transaction.getId()),
                    transaction.getDate().toString(),
                    transaction.getType().name(),
                    transaction.getCategory().name(),
                    transaction.getAmount().toPlainString(),   // 不要用 toString()，可能变科学计数法
                    escape(transaction.getNote())));
            writer.newLine();
        }
    }
}
```

**为什么用 `toPlainString()`**：`BigDecimal.toString()` 在特定场景会输出 `1E+3` 这种科学计数法，CSV 里打开就错了。

---

## 3. JUnit 5：够你自测的最小集合

```java
class MoneyBookServiceTest {

    private MoneyBookService service;

    @BeforeEach                       // 每个 @Test 之前都执行，保证测试之间互不影响
    void setUp() {
        service = new InMemoryMoneyBookServiceSolution();
    }

    @Test
    @DisplayName("余额 = 收入合计 - 支出合计")     // 中文描述，报告里看得懂
    void balance_shouldBeIncomeMinusExpense() {
        assertEquals(0, new BigDecimal("35510.50").compareTo(service.balance()));
    }

    @Test
    void exportCsv_shouldWriteHeaderAndAllRows(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("moneybook.csv");   // 临时目录，测完自动清理
        service.exportCsv(csv);
        List<String> lines = Files.readAllLines(csv);
        assertEquals(6, lines.size());
        assertEquals("id,date,type,category,amount,note", lines.get(0));
    }
}
```

常用断言：

| 断言 | 用途 |
|---|---|
| `assertEquals(expected, actual)` | 相等 |
| `assertTrue` / `assertFalse` | 布尔 |
| `assertNull` / `assertNotNull` | null |
| `assertThrows(XxxException.class, () -> ...)` | 抛异常（Lambda 里放可能抛异常的代码） |
| `assertEquals(0, a.compareTo(b))` | BigDecimal 数值比较 |

**测试的三个原则**：

1. 一个测试只验一件事，名字写清「方法_场景_期望」；
2. 测试之间不能有顺序依赖（`@BeforeEach` 重建数据）；
3. 断言写具体值，不要只写 `assertNotNull(result)` 就算完。

---

## 4. 今天的三个任务

### 4.1 实现 `exportCsv`（必做）

照第 2 节的代码写，然后：
```powershell
mvn -B test -Dtest=MoneyBookServiceTest
```
验收：8 个用例全绿，其中 `exportCsv_shouldWriteHeaderAndAllRows` 验证「6 行（1 表头 + 5 数据）+ 表头内容 + 第一行前缀」。

### 4.2 自己写 3 个测试（必做）

在 `MoneyBookServiceTest` 里加（不要照抄现有的）：

1. 空账本时 `balance()` 返回 `0`，`findAll()` 返回空列表；
2. 同一天两条支出，`findByMonth` 返回 2 条且顺序是插入顺序；
3. `total(INCOME)` 只算收入，混入支出后数值不变。

验收：`mvn -B test` 从 20 个变成 23 个，全绿。

### 4.3 限时重写（必做，30 分钟，关掉参考资料）

新建 `com/aya/learning/drills/RewriteDrill.java`，不看 `MoneyBookService` 和参考答案，写出：

- `record Expense(LocalDate date, String category, BigDecimal amount) {}`
- `static BigDecimal sumByCategory(List<Expense> list, String category)`
- `static void writeCsv(List<Expense> list, Path path) throws IOException`

验收：能编译、能跑、输出正确。写不出来就说明 Day 5/6 还没过关，明天重做。

### 4.4 读 `TaskRunner` 骨架（10 分钟）

打开 `com/aya/learning/taskrunner/TaskRunner.java` 与 `TaskRunnerDemo`，只需要回答三个问题（写进笔记）：

1. 为什么要分 `corePoolSize` / `maxPoolSize` / `queueCapacity` 三个参数？
2. `runAll` 返回「成功/失败数量」意味着任务失败不能直接抛异常，那异常去哪了？
3. 第 3 周验收标准里「提交 100 个任务，3 个抛异常，success=97」——这要求你怎么处理每个任务的异常？

（不用现在实现。第 3 周会讲 `ThreadPoolExecutor` 和 `CompletableFuture`。）

---

## 5. 本周实测输出（2026-11-04 真实运行，供你对照）

### 5.1 `mvn -B test`

```
[INFO] Running com.aya.learning.drills.DrillsTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.040 s -- in com.aya.learning.drills.DrillsTest
[INFO] Running com.aya.learning.loganalyzer.LogAnalyzerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aya.learning.loganalyzer.LogAnalyzerTest
[INFO] Running com.aya.learning.moneybook.MoneyBookServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.051 s -- in com.aya.learning.moneybook.MoneyBookServiceTest
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 5.2 `MoneyBookDemo`

```
== 全部流水 ==
#1 2026-10-08 收入 工资     18000.00  10 月工资
#2 2026-10-12 支出 餐饮        45.50  外卖
#3 2026-11-03 收入 工资     17600.00  11 月工资
#4 2026-11-03 支出 餐饮        38.00  午饭
#5 2026-11-04 支出 交通         6.00  地铁
#6 2026-11-04 支出 购物       199.00  键盘垫

== 2026-11 月度流水 ==
#3 2026-11-03 收入 工资     17600.00  11 月工资
#4 2026-11-03 支出 餐饮        38.00  午饭
#5 2026-11-04 支出 交通         6.00  地铁
#6 2026-11-04 支出 购物       199.00  键盘垫

== 支出按分类（全部数据） ==
餐饮        83.50
交通         6.00
购物       199.00

收入合计=35600.00  支出合计=288.50  余额=35311.50

== 金额从大到小 Top3 ==
#1 2026-10-08 收入 工资     18000.00  10 月工资
#3 2026-11-03 收入 工资     17600.00  11 月工资
#6 2026-11-04 支出 购物       199.00  键盘垫

CSV 已导出: C:\Develop\private\fullstack-learning\sandbox\java-basics\target\moneybook-demo.csv（7 行，含表头）
```

### 5.3 `target\moneybook-demo.csv`

```
id,date,type,category,amount,note
1,2026-10-08,INCOME,SALARY,18000.00,10 月工资
2,2026-10-12,EXPENSE,FOOD,45.50,外卖
3,2026-11-03,INCOME,SALARY,17600.00,11 月工资
4,2026-11-03,EXPENSE,FOOD,38.00,午饭
5,2026-11-04,EXPENSE,TRANSPORT,6.00,地铁
6,2026-11-04,EXPENSE,SHOPPING,199.00,键盘垫
```

### 5.4 `LogAnalyzerDemo` / `TaskRunnerDemo`

日志分析器（第 2 周主线，本周只看它能跑）：

```
== 原始行数 8 条 ==
== 级别统计（坏行已跳过） ==
DEBUG 0
INFO  2
WARN  1
ERROR 3
== Top 2 ERROR 消息 ==
1) [payment] 支付回调签名校验失败
2) [order] 订单状态非法: CANCELLED -> PAID
```

调度器骨架（第 3 周主线，本周只读）：

```
TaskRunner 骨架（第 3 周实现）
构造参数：corePoolSize / maxPoolSize / queueCapacity
待实现：ThreadPoolExecutor、拒绝策略、超时、CompletableFuture 聚合、优雅关闭
当前构造器会抛 UnsupportedOperationException，这是故意的。
第 3 周验收：提交 100 个任务（其中 3 个抛异常），结果 success=97 failure=3。
```

---

## 6. 常见错误与排错

| 现象 | 原因 | 怎么修 |
|---|---|---|
| 导出的 CSV 中文乱码 | 没指定 UTF-8，用了平台默认编码 | `StandardCharsets.UTF_8` |
| 文件写了一半是空的 | `BufferedWriter` 没 close/flush | try-with-resources |
| `NoSuchFileException` | 父目录不存在 | `Files.createDirectories(path.getParent())` |
| `AccessDeniedException` | 文件被别的程序独占（Excel 打开、杀软扫描） | 关掉占用程序；或写到新文件名 |
| 测试互相影响 | 共用同一个 service 实例，数据累积 | `@BeforeEach` 里重建 |
| `assertEquals` 比 BigDecimal 失败 | `equals` 比 scale | `assertEquals(0, expected.compareTo(actual))` |
| CSV 行数比预期多 1 | 没算表头 | `readAllLines` 返回的行数含表头 |
| `target` 下文件被 IDE 锁 | 在编辑器里开着该文件 | 关闭编辑器标签 |

---

## 7. 本周面试问题快问快答

| 问题 | 一句话答案 |
|---|---|
| Java 是值传递还是引用传递 | 值传递；对象传的是引用的副本 |
| `==` 和 `equals` | 引用 vs 内容 |
| `String` 为什么不可变 | 可共享、可做 key、线程安全；改动返回新对象 |
| `ArrayList` 扩容 | 默认 10、1.5 倍、`Arrays.copyOf` |
| `HashMap` 结构 | 数组 + 链表/红黑树，0.75 扩容，树化 8 |
| checked vs unchecked | 编译期强制 vs 运行时；`IOException` 是 checked |
| 接口 vs 抽象类 | 契约 vs 有共同实现 |
| `BigDecimal` 为什么精确 | 整数 + scale，不走二进制浮点 |
| `try-with-resources` | 自动 close，替代 finally |
| JUnit 怎么测异常 | `assertThrows(X.class, () -> ...)` |

---

## 8. 本周结束动作

1. `mvn -B test` 全绿（23 个，如果你做了 4.2）；
2. 自己的 `InMemoryMoneyBookService` 替换测试里的实现，仍全绿；
3. 笔记补完：第 1 周的 3 个「原来如此」（建议写 `==`/`equals`、值传递、Stream 一次性）；
4. 把 `index.md` 第 4 节验收清单逐项打勾，不满足的项写明「补哪一天」；
5. 满足 `index.md` 第 7 节的标准后，再开始第 2 周（日志分析器 + 泛型 + 文件流）。
