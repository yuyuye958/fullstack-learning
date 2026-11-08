# 第 1 周：Java 语言基础（语法 / 面向对象 / 异常 + 够用的集合与 IO）

- 适用对象：8 年前端（Vue/JS），后端零基础；每天可投入 4–6 小时
- 对应计划：`fullstack-ai-agent-learning-plan.md` 5.5 节 Week 1（语法 + 面向对象 + 异常），只在「记账本需要」的范围内提前用到集合、Lambda/Stream、文件读写
- 练习工程：`C:\Develop\private\fullstack-learning\sandbox\java-basics`
- 本周主线产出：可运行的 CLI 记账本 `MoneyBook` + 20 个 JUnit 5 测试全绿

> 一句话策略：**本周不求"学完 Java"，只求"能读写 Spring 代码的最小语言能力"**。泛型高级用法、Stream 进阶、NIO、并发实现、JVM 细节全部留给第 2、3 周和 Phase 2，不要提前啃。

---

## 1. 本周目标（可验证，不是"了解"）

1. 能解释并复现 6 个「JS 直觉会骗人」的点：`==` vs `equals`、基本类型与包装类、值传递、字符串不可变、`null` 与 NPE、`double` 算钱。
2. 能自己写出「类 + 构造器 + 封装 + 接口 + 枚举 + 自定义异常」的组合，不需要照着教程抄。
3. 能用 `List`/`Map`/`Comparator` 完成增删查、筛选、分组统计、排序。
4. 能用 `Files` + `try-with-resources` 读写文本文件，处理受检异常。
5. 能写 JUnit 5 测试，用 `mvn test` 判断自己写对没有。
6. 完成 `MoneyBook`：新增收支、按月筛选、按分类统计、余额、金额倒序、导出 CSV。

---

## 2. 七天日历

| 天 | 主题 | 建议时长 | 当天产出 |
|---|---|---|---|
| Day 1 | 工具链闭环、基本类型、String、`==`/`equals`、值传递 | 5h | `DrillDay1` 四个方法写对；能在 `jshell` 里演示字符串池与 Integer 缓存 |
| Day 2 | 类、对象、构造器、封装、`static`/`final`、POJO | 4.5h | `DrillDay2`；`InMemoryMoneyBookService.add()`；给 `Transaction` 加不可变 `withNote` |
| Day 3 | 继承、多态、抽象类、接口、default 方法、枚举 | 5h | `Category.fromDisplayName`；`findAll()`；能讲清「接口 vs 抽象类」 |
| Day 4 | 异常体系、checked/unchecked、try-with-resources、自定义异常 | 4.5h | `DrillDay4.parseAmount`；`add()` 的参数校验；`readAllLines` 读文件练习 |
| Day 5 | 集合：List/Set/Map、ArrayList vs LinkedList、HashMap 结构、Comparator | 6h | `findByMonth`（先 for 循环）、`totalByCategory`、`sortedByAmountDesc`；两个坑复现 |
| Day 6 | Lambda/Stream/Optional、BigDecimal、日期时间 | 5h | `DrillDay6`；`balance()`；用 Stream 重写上一步的两个方法 |
| Day 7 | 文件 IO + CSV、JUnit 5、综合验收、自测题 | 5h | `exportCsv()`；自写 3 个测试；`mvn test` 20 个全绿；自测题 ≥ 8/12 |

每天固定节奏（可按比例压缩）：

| 时段 | 时长 | 干什么 |
|---|---|---|
| 早上 | 0.5h | 复习昨天的笔记，把昨天写错的一个方法重写一遍 |
| 上午 | 2h | 读当天讲义 + 在 `jshell`/练习类里敲示例 |
| 下午 | 2–2.5h | 写当天练习，跑到测试绿 |
| 晚上 | 0.5h | 写 5 行笔记：今天学会了什么 / 哪个坑踩了 / 明天先做什么 |

---

## 3. 练习工程怎么用

位置：`C:\Develop\private\fullstack-learning\sandbox\java-basics`

```
src/main/java/com/aya/learning/
├── drills/                 # 每天的小练习（方法体是 TODO，抛 UnsupportedOperationException）
│   ├── DrillDay1.java      # == / equals / int 除法 / Integer 缓存
│   ├── DrillDay2.java      # 值传递、重新赋值
│   ├── DrillDay4.java      # 金额解析 + 自定义异常
│   ├── DrillDay6.java      # double vs BigDecimal、日期格式化
│   └── answers/Answers.java            # 四个 drill 的参考答案（先做后看）
├── moneybook/              # 本周主线：CLI 记账本
│   ├── Category.java / TransactionType.java      # 枚举
│   ├── Transaction.java                          # POJO（已给全，重点读构造器校验）
│   ├── MoneyBookService.java                     # 接口（含 default 方法）
│   ├── InMemoryMoneyBookService.java              # ★ 你要实现的骨架（全是 TODO）
│   └── solution/InMemoryMoneyBookServiceSolution.java   # 参考答案
│   └── demo/MoneyBookDemo.java                   # 可运行演示（先用参考答案）
├── loganalyzer/            # 第 2 周的主线，本周只做内存版练习
│   ├── LogLevel.java / LogEntry.java / LogAnalyzer.java   # 骨架（TODO）
│   └── solution/LogAnalyzerSolution.java + demo/LogAnalyzerDemo.java
└── taskrunner/             # 第 3 周主线，本周只读骨架
    └── TaskRunner.java + demo/TaskRunnerDemo.java
src/test/java/com/aya/learning/
├── drills/DrillsTest.java              # 8 个
├── moneybook/MoneyBookServiceTest.java # 8 个
└── loganalyzer/LogAnalyzerTest.java    # 4 个
```

常用命令（PowerShell，每次新开窗口都要先设这两个变量）：

```powershell
cd C:\Develop\private\fullstack-learning\sandbox\java-basics
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:Path="$env:JAVA_HOME\bin;C:\Develop\private\fullstack-learning\tools\apache-maven-3.9.9\bin;"+$env:Path

mvn -B test                                   # 全量测试
mvn -B test -Dtest=DrillsTest                 # 只跑一个测试类
mvn -q compile                                # 只编译

# 跑三个可运行程序（注意 JVM 参数要加引号，PowerShell 下不加引号会被拆坏）
java '-Dfile.encoding=UTF-8' -cp target/classes com.aya.learning.moneybook.demo.MoneyBookDemo
java '-Dfile.encoding=UTF-8' -cp target/classes com.aya.learning.loganalyzer.demo.LogAnalyzerDemo
java '-Dfile.encoding=UTF-8' -cp target/classes com.aya.learning.taskrunner.demo.TaskRunnerDemo
```

**完成一个练习的判定方式**：把对应测试里的 `Answers.xxx`（或 `...Solution`）换成你自己的类，跑测试。绿 = 行为一致；红 = 看断言差异。

本周实测基线（2026-11-04 真实运行结果，供你对照）：

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

---

## 4. 每日验收清单（做完打勾）

- [ ] Day 1：能当场说出 `"a" == new String("a")` 的结果和原因；`DrillDay1` 四个方法写对
- [ ] Day 2：能解释「在方法里改对象字段」和「在方法里给参数重新赋值」为什么结果不同；`add()` 通过测试
- [ ] Day 3：能讲清接口和抽象类各用在什么场景；`Category.fromDisplayName` 通过测试
- [ ] Day 4：能说出 `IOException` 和 `InvalidAmountException` 分别属于哪类异常；`parseAmount` 通过测试
- [ ] Day 5：能画 HashMap 的「数组 + 链表/红黑树」草图；`findByMonth`、`totalByCategory`、`sortedByAmountDesc` 通过测试
- [ ] Day 6：能解释 `BigDecimal.equals` 与 `compareTo` 的差别；`balance()` 通过测试；`DrillDay6` 通过
- [ ] Day 7：`exportCsv()` 通过测试；`mvn test` 20 个全绿；`MoneyBookDemo` 输出与参考答案一致
- [ ] 综合：不看任何资料，30 分钟内写出「类 + 接口 + 枚举 + 异常 + List + 文件写」的 80 行小程序

---

## 5. 自测题（先自己答，答案在下一节）

1. `String a = "a"; String b = new String("a");` `a == b`、`a.equals(b)` 分别是？
2. 为什么 `int` 的 `5 / 2` 是 2？写出两种得到 2.5 的写法。
3. Java 是值传递还是引用传递？用一段代码解释。
4. `Integer a = 127, b = 127` 时 `a == b` 是 true，`1000` 时是 false，为什么？
5. 为什么重写 `equals` 必须重写 `hashCode`？不重写会出什么问题？
6. checked exception 和 unchecked exception 的区别？`IOException` 属于哪种？Spring `@Transactional` 默认对哪些异常回滚？
7. `List.of("a")` 和 `Arrays.asList("a")` 各能不能 `add`？
8. 接口和抽象类的区别？接口的 `default` 方法解决什么问题？
9. 为什么金额不能用 `double`？`new BigDecimal(0.1)` 有什么问题？
10. `String` 为什么不可变？循环里用 `+` 拼接有什么问题？
11. `try-with-resources` 解决了什么问题？哪些类能用？
12. `findAll()` 为什么要返回 `List.copyOf(...)` 而不是直接返回内部 `ArrayList`？

---

## 6. 自测题答案

1. `a == b` 为 **false**（b 是 `new` 出来的新对象，不在字符串常量池），`a.equals(b)` 为 **true**（比内容）。
2. 两个 `int` 做 `/` 会先按整数除法截断。得到 2.5：`(double) a / b` 或 `a / 2.0`。
3. **值传递**。传对象时传的是引用值的副本：方法内改字段外部可见（同一对象），方法内把参数重新指向新对象外部不可见。
4. `Integer.valueOf()` 对 **-128~127** 有缓存，范围内返回同一对象，超出则 `new Integer` 每次都是新对象；`==` 比引用。
5. `HashMap` 先用 `hashCode` 定位桶、再用 `equals` 比较；只重写 `equals` 会让「相等的两个对象」落到不同桶，`map.get(key)` 拿不到值、`HashSet` 去重失效。
6. checked 在编译期强制处理（`IOException` 属于它）；unchecked 是 `RuntimeException` 及子类，编译期不强制。Spring 默认只在 **`RuntimeException` 和 `Error`** 时回滚，checked 异常默认不回滚（要写 `rollbackFor`，mall 里就是这么写的）。
7. `List.of` **不能**（完全不可变）；`Arrays.asList` **不能 add/remove**（固定大小，但可以 `set`）。要可变用 `new ArrayList<>(Arrays.asList(...))`。
8. 抽象类可以有实现、字段、构造器，适合「有共同实现」；接口定义契约，适合「面向接口编程」。`default` 让接口在不破坏已有实现的前提下新增方法（JDK 8 引入）。
9. `double` 是二进制浮点，`0.1` 无法精确表示，累加会出 `0.30000000000000004`；金额用 `BigDecimal`，且必须从**字符串**构造——`new BigDecimal(0.1)` 会把 double 的误差原样带进来。
10. `String` 不可变才能安全地共享（常量池）、做 HashMap 的 key、线程安全。循环里 `+` 每轮都产生新对象，用 `StringBuilder`。
11. 自动关闭实现了 `AutoCloseable` 的资源（流、连接、锁），即使抛异常也会 `close()`，替代容易写错的 `finally`。
12. 防止外部直接拿到内部集合后 `add/remove`，破坏封装与「快照」语义；`List.copyOf` 返回不可变副本，外部改不动内部数据。

---

## 7. 达到什么程度才可以进第 2 周

全部满足再进，否则把 Day 5–7 重做一遍（**不要**带着「大概懂了」往下走，第 2 周的 Stream/IO 会直接崩）：

- [ ] `mvn test` 20 个测试全绿，而且你知道每个测试在验什么
- [ ] 把 `MoneyBookServiceTest` 里的实现类换成你自己写的 `InMemoryMoneyBookService`，仍然全绿
- [ ] `MoneyBookDemo` 能跑，输出与参考答案一致（数字、行数、CSV 行数都对得上）
- [ ] 自测题答对 ≥ 8/12，答错的能翻回对应 day 文件找到解释
- [ ] 能不看资料写出「新增一条流水 + 按分类合计 + 写 CSV」三段代码，允许查 API 名但不容许抄结构

---

## 8. 本周刻意不做（留到后面）

| 内容 | 放到哪 |
|---|---|
| 泛型深入（通配符、类型擦除、自写泛型容器） | 第 2 周（集合/泛型） |
| Stream 进阶（`flatMap`、并行流、自定义 Collector） | 第 2 周（日志分析器） |
| 文件大文件流式读取、NIO、CSV 复杂转义 | 第 2 周 |
| 并发（线程池参数、`CompletableFuture`、`volatile`） | 第 3 周（`TaskRunner`） |
| JVM 内存/GC | 第 3 周（只看概念） |
| Mockito、多模块 Maven | 第 3 周 |
| Spring / MyBatis / 事务传播 | Phase 2（不要本周碰） |
