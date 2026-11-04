# Day 6：Lambda、Stream、Optional、BigDecimal、日期时间

> 时长 5h：上午 2h 概念，下午 2.5h 改代码 + 练习，晚上 0.5h 笔记

---

## 1. Lambda 与函数式接口

```java
// 老写法：匿名内部类
list.sort(new Comparator<Transaction>() {
    @Override
    public int compare(Transaction a, Transaction b) {
        return a.getAmount().compareTo(b.getAmount());
    }
});

// Lambda
list.sort((a, b) -> a.getAmount().compareTo(b.getAmount()));

// 方法引用
list.sort(Comparator.comparing(Transaction::getAmount));
```

Lambda 只能赋值给**函数式接口**（只有一个抽象方法的接口），常用的四个：

| 接口 | 签名 | 用途 |
|---|---|---|
| `Function<T,R>` | `R apply(T)` | 转换 |
| `Predicate<T>` | `boolean test(T)` | 筛选 |
| `Consumer<T>` | `void accept(T)` | 消费（打印、保存） |
| `Supplier<T>` | `T get()` | 提供（延迟创建） |

**关键差异（JS 会骗你）**：Lambda 里引用的外部局部变量必须是 **effectively final**（不能被重新赋值）。

```java
int total = 0;
list.forEach(t -> total += 1);     // 编译错误：total 不是 effectively final
```

JS 闭包可以随便改外部变量；Java 的 lambda 捕获的是**值的副本**，所以干脆禁止你改。

---

## 2. Stream：一条流水线，只能用一次

```java
List<Transaction> nov = transactions.stream()
        .filter(t -> YearMonth.from(t.getDate()).equals(Month.NOVEMBER))
        .sorted(Comparator.comparing(Transaction::getAmount).reversed())
        .toList();
```

三个阶段：**创建**（`stream()`）→ **中间操作**（`filter`/`map`/`sorted`/`distinct`/`limit`，返回新 Stream，惰性）→ **终止操作**（`toList`/`collect`/`count`/`reduce`/`forEach`，触发执行）。

常用操作速查：

```java
.map(Transaction::getAmount)                 // 转换
.filter(t -> t.getType() == EXPENSE)         // 筛选
.sorted(Comparator...)                       // 排序
.distinct()                                  // 去重（靠 equals/hashCode）
.limit(3)                                    // 取前 N
.collect(Collectors.groupingBy(Transaction::getCategory))
.collect(Collectors.groupingBy(..., Collectors.counting()))
.reduce(BigDecimal.ZERO, BigDecimal::add)    // 归约
.anyMatch(...) / .allMatch(...) / .noneMatch(...)
```

**两个坑**：

1. Stream **只能消费一次**，第二次调终止操作抛 `IllegalStateException: stream has already been operated upon or closed`。
2. Stream 不改变原集合（`sorted` 返回新 Stream），要拿到结果必须 `toList()`/`collect`。

**前端视角的坑**：JS 数组的 `map/filter` 每次返回新数组、可以反复用；Java Stream 是一次性的管道。

**到什么深度**：会写 `filter + map + sorted + collect` 就够用；`flatMap`、并行流、自定义 Collector 留到第 2 周。

---

## 3. Optional：表达「可能没有」

```java
Optional<Transaction> found = transactions.stream()
        .filter(t -> t.getId() == id)
        .findFirst();

found.ifPresent(t -> System.out.println(t));
found.orElseThrow(() -> new MoneyBookException("流水不存在：" + id));
found.map(Transaction::getAmount).orElse(BigDecimal.ZERO);
```

**解决的问题**：把「可能返回 null」写进方法签名，逼调用方处理。**不解决的问题**：它不能替代所有 null 判断，也不该用作字段类型或方法参数（《Effective Java》的建议）。

坑：`Optional.get()` 在空时抛 `NoSuchElementException`，能不用就不用，改用 `orElse`/`orElseThrow`/`ifPresent`。

---

## 4. BigDecimal：金额唯一正确姿势

```java
BigDecimal a = new BigDecimal("0.1");   // ✅ 从字符串
BigDecimal b = new BigDecimal(0.1);     // ❌ 把 double 的误差带进来（0.1000000000000000055...）

a.add(b);            // 加
a.subtract(b);       // 减
a.multiply(b);       // 乘
a.divide(b, 2, RoundingMode.HALF_UP);   // 除：必须指定精度和舍入，否则可能抛 ArithmeticException
a.setScale(2, RoundingMode.HALF_UP);    // 保留两位
a.signum();          // 正负：-1 / 0 / 1，比 compareTo(0) 更直观
```

**`equals` vs `compareTo`**：

```java
new BigDecimal("1.0").equals(new BigDecimal("1.00"))   // false（scale 不同）
new BigDecimal("1.0").compareTo(new BigDecimal("1.00")) // 0（数值相等）
```

所以断言金额用 `compareTo(...) == 0`，不要用 `assertEquals` 直接比对象。你在 `MoneyBookServiceTest` 里看到的全是 `assertEquals(0, expected.compareTo(actual))`，就是这个原因。

**舍入模式**：`HALF_UP` 是四舍五入（银行/电商常用），`HALF_EVEN` 是银行家舍入（减少统计偏差，金融精算用）。

---

## 5. 日期时间：java.time，别再用 Date/SimpleDateFormat

```java
LocalDate today = LocalDate.of(2026, 11, 4);      // 只有日期
LocalDate next = today.plusDays(1);
YearMonth month = YearMonth.from(today);          // 2026-11
LocalDateTime now = LocalDateTime.now();
Duration d = Duration.between(start, end);        // 时间差

DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
String text = fmt.format(today);                  // 2026年11月04日
LocalDate parsed = LocalDate.parse("2026-11-04"); // ISO 格式直接 parse
```

为什么不用 `Date`：可变（`setTime`）、月份从 0 开始、`SimpleDateFormat` 非线程安全。`java.time` 全部不可变、线程安全。

**什么时候用**：所有业务日期（下单时间、统计月份）。数据库里存 `DATETIME`/`DATE`，MyBatis 能直接映射到 `LocalDateTime`/`LocalDate`。

---

## 6. 今天要改的代码

### 6.1 `DrillDay6`（三题）

```java
public static double doubleSumProblem() {
    return 0.1 + 0.1 + 0.1;            // 0.30000000000000004
}

public static BigDecimal bigDecimalSumFixed() {
    BigDecimal cent = new BigDecimal("0.1");
    return cent.add(cent).add(cent);
}

public static String formatDate(LocalDate date) {
    return DateTimeFormatter.ofPattern("yyyy年MM月dd日").format(date);
}
```

验收：`DrillsTest` 里 3 个 day6 相关断言换掉 `Answers` 后跑绿。

### 6.2 `balance()`（用 reduce）

```java
@Override
public BigDecimal balance() {
    return transactions.stream()
            .map(Transaction::getSignedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

验收：`MoneyBookServiceTest.balance_shouldBeIncomeMinusExpense` 绿（35510.50）。

### 6.3 用 Stream 重写三个方法（Day 5 的 for 版留着对比）

```java
@Override
public List<Transaction> findByMonth(YearMonth month) {
    return transactions.stream()
            .filter(t -> YearMonth.from(t.getDate()).equals(month))
            .toList();
}

@Override
public Map<Category, BigDecimal> totalByCategory(TransactionType type) {
    return transactions.stream()
            .filter(t -> t.getType() == type)
            .collect(Collectors.groupingBy(Transaction::getCategory,
                    TreeMap::new,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
}

@Override
public List<Transaction> sortedByAmountDesc() {
    return transactions.stream()
            .sorted(Comparator.comparing(Transaction::getAmount).reversed())
            .toList();
}
```

验收：`mvn -B test -Dtest=MoneyBookServiceTest` 从「部分红」变成「只剩 `exportCsv` 相关红」；同时你要能回答「Stream 版和 for 版哪个好读」。

### 6.4 加一个 `findById` 返回 Optional（练习 Optional）

在接口里加：

```java
Optional<Transaction> findById(long id);
```

实现用 `stream().filter(...).findFirst()`。
验收：`findById(999)` 返回 `Optional.empty()`，`findById(1).isPresent()` 为 true，全程没有 NPE。

---

## 7. 常见错误与排错

| 现象 | 原因 | 怎么修 |
|---|---|---|
| `local variables referenced from a lambda expression must be final or effectively final` | lambda 改了外部变量 | 用 `AtomicInteger`/数组，或改成 Stream 归约 |
| `IllegalStateException: stream has already been operated upon` | 同一个 Stream 用了两次 | 每次重新 `stream()` 或先 `toList()` |
| `NoSuchElementException` | `Optional.get()` 遇到空 | 用 `orElse` / `orElseThrow` |
| `ArithmeticException: Non-terminating decimal expansion` | `divide` 没指定精度 | `divide(b, 2, RoundingMode.HALF_UP)` |
| `assertEquals` 比 BigDecimal 失败但数值相同 | `equals` 比 scale | 用 `compareTo(...) == 0` |
| `DateTimeParseException` | 字符串格式和 pattern 不匹配 | 用 ISO 格式，或显式 `DateTimeFormatter` |

---

## 8. 面试怎么问 / 到什么深度

| 问题 | 答到什么程度 |
|---|---|
| Lambda 的原理 | 函数式接口 + 编译成 `invokedynamic`；能说「本质是接口的匿名实现」即可 |
| Stream 和 for 循环怎么选 | 都可；Stream 更声明式、可读性好，简单循环/需要下标/需要 break 时用 for |
| 中间操作和终止操作 | 中间操作惰性，终止操作触发执行 |
| Optional 的正确用法 | 返回值用，别做字段/参数；别 `get()`，用 `orElse` 系列 |
| BigDecimal 为什么能精确 | 整数 + scale 表示十进制，不经过二进制浮点 |
| `setScale` 和 `RoundingMode` | 保留位数 + 舍入策略，除法必须显式指定 |
| `java.time` 相比 `Date` 好在哪 | 不可变、线程安全、API 清晰、月份从 1 开始 |

**本周不做**：`CompletableFuture`、并行 Stream、`flatMap` 复杂用法——第 2/3 周。
