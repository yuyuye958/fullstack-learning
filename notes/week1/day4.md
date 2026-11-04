# Day 4：异常体系与错误处理

> 时长 4.5h：上午 2h 概念，下午 2h 写 `parseAmount` + 文件异常练习，晚上 0.5h 笔记

---

## 1. Throwable 家族：先看清结构

```
Throwable
├── Error                      ← 别 catch（OOM、StackOverflow）
└── Exception
    ├── RuntimeException       ← 非受检（unchecked）
    │   ├── NullPointerException
    │   ├── IllegalArgumentException
    │   ├── IllegalStateException
    │   ├── IndexOutOfBoundsException
    │   └── 你的业务异常 MoneyBookException
    └── 其他 Exception          ← 受检（checked）
        ├── IOException
        ├── SQLException
        ├── InterruptedException
        └── ...
```

**受检（checked）= 编译器强制你处理**；**非受检（unchecked）= 编译期不管，运行时才炸**。

```java
// 编译不过：未处理的异常 IOException
static String read(Path path) {
    return Files.readString(path);
}

// 正确：声明出去，或者当场 try-catch
static String read(Path path) throws IOException {
    return Files.readString(path);
}
```

**前端视角的坑**：JS 里 `throw` 什么都行、`Promise.reject` 不处理最多控制台来个 unhandledRejection；Java 的 checked 异常是**编译期强制**，不写 `throws` 或 `try-catch` 连编译都过不了。这不是 Java 啰嗦，是它把「这个操作可能失败」写进了类型系统。

---

## 2. 五个必备语法

### 2.1 try-catch

```java
try {
    BigDecimal amount = new BigDecimal(input);
} catch (NumberFormatException e) {
    throw new InvalidAmountException("金额格式不正确：" + input, e);
}
```

### 2.2 多个 catch：从具体到宽泛

```java
try {
    ...
} catch (NumberFormatException e) {        // 具体
    ...
} catch (RuntimeException e) {             // 宽泛，必须放后面
    ...
}
```

顺序写反（宽泛在前）编译直接报错：后面的 catch 永远到不了。

### 2.3 finally

```java
try {
    ...
} finally {
    // 一定会执行（即使 return / 抛异常），常用于释放资源
}
```

**坑**：`finally` 里写 `return` 会吞掉异常，尽量不要这么写。

### 2.4 try-with-resources（必须掌握）

```java
try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
    return reader.readLine();
}
```

只要资源实现了 `AutoCloseable`（流、`Connection`、`Lock`），就会在作用域结束时自动 `close()`，即使中途抛异常。等价于「finally 里 close 且不会吞异常」，但短得多。

### 2.5 throw / throws

- `throw`：抛出一个异常对象（在方法体里）；
- `throws`：在方法签名上声明「我可能抛这些异常，调用方自己处理」。

```java
public void exportCsv(Path path) throws IOException   // 声明
```

---

## 3. 自定义异常：继承谁？

```java
public class MoneyBookException extends RuntimeException { ... }
public class InvalidAmountException extends MoneyBookException { ... }
```

两个决策点：

1. **继承 RuntimeException**：业务规则失败（金额非法、库存不足）调用方通常无法恢复，也不该被迫 try-catch；而且 Spring 事务默认只对 `RuntimeException` 回滚。
2. **拆子类**：`catch (InvalidAmountException e)` 能精确处理，不会把其他业务异常一起吞掉。

**异常链**：包装底层异常时把 cause 传进去，排错时能看到真实原因：

```java
throw new InvalidAmountException("金额格式不正确：" + raw, e);
```

日志里会打印 `Caused by: java.lang.NumberFormatException: For input string: "abc"`——mall 里 `/api/products/abc` 那个 500 缺陷就是靠这条链定位到 `Long.parseLong` 的。

---

## 4. 三条铁律

1. **不要空 catch**：

   ```java
   try { ... } catch (Exception e) { }   // 最糟糕的写法，问题被静默
   ```

2. **catch 之后要么处理，要么包装再抛**，不能既不打日志也不抛。
3. **异常信息要能定位问题**：`"金额非法"` 不够，要 `"金额必须大于 0：-3"`。

---

## 5. 受检异常 vs Promise reject（对照表）

| 维度 | JS `Promise.reject` | Java checked exception |
|---|---|---|
| 谁来保证被处理 | 没人，靠自觉；未处理只是控制台警告 | 编译器强制，不处理编译失败 |
| 传播方式 | `await` 处 try-catch，或链式 `.catch` | `throws` 逐层声明 |
| 丢失风险 | 忘记 `await` 会变成 unhandledRejection | 忘记处理根本编译不过 |
| 适合什么 | 异步 IO、网络请求 | 同步 IO、数据库、文件 |

**Java 的异步异常**（`CompletableFuture`）和 Promise 更像：异常包在 `future` 里，`join()` 才抛 `CompletionException`。这部分第 3 周讲。

---

## 6. 今天写的代码

### 6.1 `DrillDay4.parseAmount`（必做）

```java
public static BigDecimal parseAmount(String raw) {
    if (raw == null || raw.isBlank()) {
        throw new InvalidAmountException("金额不能为空");
    }
    BigDecimal amount;
    try {
        amount = new BigDecimal(raw.trim());
    } catch (NumberFormatException e) {
        throw new InvalidAmountException("金额格式不正确：" + raw, e);
    }
    if (amount.signum() <= 0) {
        throw new InvalidAmountException("金额必须大于 0：" + raw);
    }
    return amount.setScale(2, RoundingMode.HALF_UP);
}
```

验收：`DrillsTest.day4_parseAmount` 换成 `DrillDay4` 后跑绿（5 个断言：正常值、`abc`、`-1`、`null`、`0`）。

### 6.2 给 `add()` 补参数校验（必做）

现在的 `add` 靠 `Transaction` 构造器校验，但 `date` 只做了 `Objects.requireNonNull`（抛 `NullPointerException`）。改造成：

```java
if (date == null) {
    throw new MoneyBookException("日期不能为空");
}
```

验收：`jshell` 里调 `service.add(null, ...)`，看到的是 `MoneyBookException` 而不是 `NullPointerException`——**异常类型本身就是 API 的一部分**。

### 6.3 文件读取练习（必做，为 Day 7 铺路）

新建 `com/aya/learning/drills/FileDrill.java`：

```java
public static List<String> readAllLines(Path path) throws IOException {
    return Files.readAllLines(path, StandardCharsets.UTF_8);
}
```

再写一个 `main`，分别传：存在的文件（`pom.xml`）、不存在的文件（`nope.txt`）。
验收：第二个跑到 `NoSuchFileException`，你能在栈里找到 `readAllLines` → `main` 的调用链；然后加 try-catch 打印一句人话「文件不存在：nope.txt」。

---

## 7. 常见错误与排错

| 现象 | 原因 | 怎么修 |
|---|---|---|
| `unreported exception java.io.IOException; must be caught or declared` | 调了受检异常方法但没处理 | 加 `throws` 或 `try-catch` |
| `exception ... is never thrown in body of corresponding try statement` | catch 了不可能抛的异常 | 删掉多余 catch，或检查方法签名 |
| `catch (Exception e)` 后什么都看不到 | 吞异常 | 至少 `log.error("...", e)` 或用 `throw new Xxx(..., e)` |
| `StackOverflowError` 出现 | Error 级别，不该 catch | 查递归 |
| 异常栈只看到 wrapper，看不到根因 | 没传 cause | `new Xxx(msg, e)` |
| `finally` 里 return 导致结果被覆盖 | `finally` 的 return 会覆盖 try 的 return | 去掉 |

---

## 8. 面试怎么问 / 到什么深度

| 问题 | 答到什么程度 |
|---|---|
| checked / unchecked 区别 | 见第 1 节；补一句：checked 需要 `throws` 或 try-catch，unchecked 是编程错误（NPE、越界） |
| `try-with-resources` 原理 | 编译成 `try/finally` + `AutoCloseable.close()`，异常抑制用 `addSuppressed` |
| 异常处理原则 | 不吞异常、快速失败、异常信息含上下文、业务异常与系统异常分开 |
| Spring 事务默认回滚哪些 | `RuntimeException` 和 `Error`；checked 需要 `@Transactional(rollbackFor = Exception.class)`——mall 的 `OrderCreateTransactionService` 就是显式写了 |
| 什么时候自定义异常 | 需要调用方精确捕获 / 需要携带业务码 / 需要统一转换 HTTP 状态 |

**本周不做**：全局异常处理器、`@ControllerAdvice`、事务传播——那是 Phase 2 的事（mall 里已有现成代码，到时对照学）。
