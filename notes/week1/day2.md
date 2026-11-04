# Day 2：类、对象、构造器与封装

> 时长 4.5h：上午 2h 概念 + 读 `Transaction`，下午 2h 写 `add()`，晚上 0.5h 笔记
> 今天开始碰主线：`com.aya.learning.moneybook`。

---

## 1. 类和对象：`new` 之后发生了什么

```java
Transaction t = new Transaction(1, LocalDate.of(2026, 11, 4),
        TransactionType.EXPENSE, Category.FOOD, new BigDecimal("38"), "午饭");
```

三件事：

1. JVM 在**堆**上分配对象内存，字段先给默认值（`0` / `null` / `false`）；
2. 执行构造器，给字段赋真正的值；
3. 把对象地址（引用）赋给变量 `t`——`t` 本身在栈上，只是一根「指针」。

JS 里 `const t = { ... }` 看起来一样，但 Java 的字段有明确类型、访问修饰符、初始化顺序。**初始化顺序**（面试会问）：静态字段 → 静态代码块 → 实例字段 → 实例代码块 → 构造器。

**什么时候用**：所有业务对象（`Transaction`、`Product`、`Order`）都是这么来的；Spring 帮你 new 的是 `@Service`/`@Component` 标注的类。

---

## 2. 字段、方法、构造器、`this`

```java
public class Transaction {
    private final long id;          // 字段：对象的状态
    private final BigDecimal amount;

    public Transaction(long id, ..., BigDecimal amount, ...) {   // 构造器：与类同名，无返回值
        this.id = id;               // this = 当前对象，用来区分同名的参数
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAmount() { // 方法：对象的行为/访问器
        return amount;
    }
}
```

注意三个硬规则：

- 构造器**没有返回值**，连 `void` 都不能写；
- 构造器名必须和类名完全一致；
- 每个类至少有一个构造器，不写就送你一个无参空构造器；一旦自己写了，默认那个就没了。

**前端视角最容易误解的点**：JS 对象可以随便加字段，Java 不行——字段在编译期就固定了，想加就得改类。

---

## 3. 封装：为什么字段是 `private`

```java
private final BigDecimal amount;
```

- `private`：只有本类能访问；
- `final`：只能赋值一次（构造器里），之后不能改；
- 对外只暴露 `getAmount()`，不暴露 setter。

好处不是「显得规范」，而是：**外部改不了内部状态，你就不用满项目找「谁把它改坏了」**。

`Transaction` 的构造器里做了三件事，值得逐行读：

```java
this.date = Objects.requireNonNull(date, "date 不能为 null");
if (amount == null || amount.signum() <= 0) {
    throw new InvalidAmountException("金额必须大于 0：" + amount);
}
this.amount = amount.setScale(2, RoundingMode.HALF_UP);
```

1. **校验前置**：不合法的对象根本创建不出来（避免「半成品对象」在系统里流转）；
2. **统一精度**：所有金额到 `Transaction` 这一层就已经是两位小数；
3. **失败即抛异常**：Day 4 会细讲为什么用异常而不是返回 `false`。

**什么时候用**：DTO / Entity / 值对象都应该这样；**什么时候不用**：纯内部临时计算的小类可以简单点。

---

## 4. `static` 与 `final`

| 关键字 | 含义 | 例子 |
|---|---|---|
| `static` 字段 | 类级共享，所有对象一份 | 常量、缓存、计数器 |
| `static` 方法 | 不依赖对象，用 `类名.方法()` 调 | `Objects.requireNonNull`、`Integer.valueOf` |
| `final` 字段 | 只能赋值一次 | `Transaction.id` |
| `final` 方法 | 不能被重写 | 框架里防止子类改行为 |
| `static final` | 常量 | `private static final String KEY_PREFIX = "mall:product:detail:"` |

`static` 方法里**不能**直接用实例字段（没有对象，哪来的字段）：

```java
public class Foo {
    private int count;                 // 实例字段

    public static void bad() {
        count++;                       // 编译错误：non-static variable count
    }
}
```

**面试怎么问**：`static` 变量和实例变量的区别？`static` 方法能不能访问 `this`？答案：不能。

**到什么深度**：知道 `static` 是类级共享、多线程下不安全（第 3 周会写两个线程改 `static` 计数器复现）；知道 `static final` 是常量。**不用**背类加载时机。

---

## 5. POJO / JavaBean / record：后端最常见的三种「数据类」

| 类型 | 特征 | 本项目里的例子 |
|---|---|---|
| POJO | 普通对象，字段 + getter/setter，不实现特殊接口 | `Transaction`（字段是 final，更接近不可变值对象） |
| JavaBean | 无参构造 + private 字段 + public getter/setter + 命名规范 | Spring MVC 的请求/响应 DTO |
| record（JDK 16+） | 一行声明，自动生成构造器/getter/equals/hashCode/toString，不可变 | 第 3 周的 `Result(int success, int failure)` |

```java
public record Point(int x, int y) { }     // 一行等于几十行样板代码
```

**什么时候用**：纯数据载体优先 `record`；需要 Jackson 反序列化、需要 setter 的 DTO 用 JavaBean；有校验逻辑用「构造器校验 + final 字段」（像 `Transaction`）。

**面试怎么问**：「POJO 和 JavaBean 的区别？」答：JavaBean 有更严格的规范（无参构造、getter/setter 命名），POJO 只要是个普通对象即可；现在还会追问 record 和 Lombok。

---

## 6. 不可变对象：`withXxx` 而不是 setter

`Transaction` 全是 final 字段。想「改备注」怎么办？不是加 setter，而是返回一个新对象：

```java
public Transaction withNote(String newNote) {
    return new Transaction(id, date, type, category, amount, newNote);
}
```

好处：原对象永远不被改，多线程安全，缓存的 key 不会变。

**什么时候用**：金额、订单、配置；**什么时候不用**：需要频繁局部修改的大对象（性能敏感）。

---

## 7. 今天要写的代码

### 7.1 `InMemoryMoneyBookService.add()`（15 行以内）

打开 `src/main/java/com/aya/learning/moneybook/InMemoryMoneyBookService.java`，把第一个 TODO 换成：

```java
@Override
public Transaction add(LocalDate date, TransactionType type, Category category, BigDecimal amount, String note) {
    long id = ++sequence;
    Transaction transaction = new Transaction(id, date, type, category, amount, note);
    transactions.add(transaction);
    return transaction;
}
```

验收方式：把 `MoneyBookServiceTest` 里的 `new InMemoryMoneyBookServiceSolution()` 改成 `new InMemoryMoneyBookService()`，然后：

```powershell
mvn -B test -Dtest=MoneyBookServiceTest
```

此时应该只有依赖 `findAll/findByMonth/...` 的用例红（那几个方法还没实现），`add_shouldAssignIdAndScale` 和 `add_shouldRejectNonPositiveAmount` 应该绿。**只红一部分**就对了——说明你的 `add` 是对的。

### 7.2 `DrillDay2`（值传递）

实现两个方法，替换 `DrillsTest.day2_passByValue` 里的 `Answers`，跑绿。

### 7.3 给 `Transaction` 加 `withNote`

按上面第 6 节加方法，然后写 3 行验证代码（`jshell` 或新写一个 `main`）：

```java
Transaction t1 = new Transaction(1, LocalDate.now(), TransactionType.EXPENSE,
        Category.FOOD, new BigDecimal("10"), "原备注");
Transaction t2 = t1.withNote("新备注");
t1.getNote();   // 应该还是「原备注」
t2.getNote();   // 「新备注」
```

验收：`t1` 没变，`t2` 是另一个对象（`t1 != t2` 为 true）。

---

## 8. 常见错误与排错

| 现象 | 原因 | 怎么修 |
|---|---|---|
| `non-static variable xxx cannot be referenced from a static context` | `static` 方法里用了实例字段/方法 | 要么把字段也 `static`，要么把方法改成实例方法 |
| `无法将类 ... 中的构造器 ... 应用到给定类型` | 参数个数/类型对不上 | 对照构造器签名；注意 `BigDecimal` 不能传 `double` 字面量而不加引号？其实是能传但会走 `double` 构造器，别这么写 |
| 构造器里 `this.id = id` 写成 `id = id` | 参数遮蔽字段，自赋值 | 永远加 `this.`（或用不同参数名） |
| `IllegalArgumentException: 金额必须大于 0：null` | `amount` 是 `null`，消息里拼接了它 | 正常，说明校验生效；调用方要保证不为 null |
| 改了 `Transaction` 后测试里的金额对不上 | `setScale(2)` 改变了 `BigDecimal.equals` 的 scale | 断言用 `compareTo(...) == 0`，不要用 `assertEquals(new BigDecimal("38"), ...)` |

---

## 9. 面试怎么问 / 到什么深度

| 问题 | 你要答到什么程度 |
|---|---|
| 面向对象三大特性 | 封装（隐藏状态、只暴露行为）、继承（复用 + 扩展）、多态（同一接口不同实现）；各配一句项目里的例子 |
| 重载和重写的区别 | 重载：同类中方法名相同、参数不同，编译期确定；重写：子类覆盖父类方法，运行期动态绑定 |
| `this` / `super` 的作用 | `this` 指当前对象（区分参数、构造器互相调用 `this(...)`）；`super` 调父类构造器/方法 |
| `static` 能不能被重写 | 不能；`static` 方法属于类，不存在多态 |
| `final` 三种用法 | 字段不可变、方法不可重写、类不可继承（`String` 就是 final 类） |
| 为什么 POJO 要有无参构造 | 反射/框架（Jackson、MyBatis）需要先 new 再 set |

**不要做的事**：背设计模式；今天只需要能说清「封装、不可变、构造器校验」三件事。
