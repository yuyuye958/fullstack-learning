# Day 3：继承、多态、接口与枚举

> 时长 5h：上午 2h 概念，下午 2.5h 写 `findAll` + 枚举练习，晚上 0.5h 笔记

---

## 1. 继承：`extends`，能少写但别乱用

```java
class Animal {
    protected String name;
    public String speak() { return "..."; }
}

class Dog extends Animal {
    @Override
    public String speak() { return "汪汪"; }
}
```

- `extends` 只能有一个父类（单继承）；接口可以多实现。
- `@Override` 不是必须的，但**强烈建议加**：方法签名写错时编译器会报错，而不是悄悄变成一个新方法。
- 子类构造器第一行会隐式调用 `super()`；父类没有无参构造时，必须显式 `super(args)`。

**什么时候用**：真的存在「is-a」关系时（`InvalidAmountException extends MoneyBookException`）。**什么时候不用**：只想复用几个方法——用组合（把对象作为字段注入），组合比继承更常见。

**前端视角的坑**：JS 的原型链是「运行时查属性」，Java 的继承在编译期就确定了方法表；`@Override` 写错不会静默通过。

---

## 2. 多态：编译看左边，运行看右边

```java
MoneyBookService service = new InMemoryMoneyBookServiceSolution();
service.balance();     // 实际执行的是实现类的方法
```

变量类型是接口，实际对象是实现类——调用时按**运行时的真实类型**找方法。这就是「面向接口编程」的全部机制。

**为什么重要**：Spring 里 `@Autowired ProductService productService` 注入了实现类，你写的代码只认接口；换实现（内存 → MySQL）不用改调用方。

---

## 3. 抽象类 vs 接口

| 维度 | 抽象类 `abstract class` | 接口 `interface` |
|---|---|---|
| 能否有实现 | 有，可以有具体方法 | 可以有 `default` / `static` 方法 |
| 字段 | 可以有实例字段 | 只能是 `public static final` 常量 |
| 构造器 | 有（给子类用） | 没有 |
| 继承/实现 | 单继承 | 可多实现 |
| 典型场景 | 有共同实现 + 模板方法 | 定义契约、能力声明 |

```java
public interface MoneyBookService {
    Transaction add(...);                          // 抽象方法：只有签名

    default Transaction addIncome(...) {           // default：给所有实现类的便利方法
        return add(date, TransactionType.INCOME, category, amount, note);
    }
}
```

**`default` 方法解决什么问题**：JDK 8 之前，接口加一个方法会让所有实现类编译失败；`default` 让接口能演进而不破坏已有实现（`List.forEach` 就是这么加的）。

**前端视角的坑**：TypeScript 的 `interface` 编译后完全消失，只做类型检查；Java 的 `interface` 是**运行时真实存在**的，有方法表、能被反射、能多态调用。这是两套东西。

**到什么深度**：能说清「有共同实现用抽象类，只定契约用接口；现代 Java 更倾向接口 + default」。**不用**背模板方法模式。

---

## 4. 枚举：不止是常量

```java
public enum Category {
    FOOD("餐饮"), TRANSPORT("交通"), SHOPPING("购物"), SALARY("工资"), OTHER("其他");

    private final String displayName;

    Category(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
```

枚举的本质是「固定实例的类」：每个枚举值都是单例对象。带来的好处：

- `switch` 可以穷举，漏掉分支编译器会提示（用 switch 表达式更严格）；
- `==` 比较永远安全（都是同一个实例），不需要 `equals`；
- 可以带字段、方法、甚至实现接口。

常用 API：

```java
Category.values()        // 所有值（返回数组，每次都是新数组副本）
Category.valueOf("FOOD") // 字符串转枚举；不认识会抛 IllegalArgumentException
Category.FOOD.name()     // "FOOD"
```

**什么时候用**：状态（订单状态、支付状态）、分类、方向、日志级别。**面试怎么问**：枚举为什么线程安全 / 枚举实现单例。答案：类加载时初始化，JVM 保证只初始化一次。

---

## 5. 内部类（读代码够用即可）

```java
public class Outer {
    private int x = 1;

    class Inner {                 // 成员内部类：持有外部类引用
        int read() { return x; }
    }

    static class Nested { }       // 静态嵌套类：不持有外部引用，最常用
}
```

你在 Spring 代码里会看到大量 `static class`（配置类、常量类）。**只需要知道**：非静态内部类隐含持有外部对象引用，容易内存泄漏；写工具类/配置类用 `static`。

**到什么深度**：能看懂，不要求自己设计。

---

## 6. 今天写的三块代码

### 6.1 `Category.fromDisplayName`（枚举反查）

```java
public static Category fromDisplayName(String name) {
    if (name == null || name.isBlank()) {
        return OTHER;
    }
    String trimmed = name.trim();
    for (Category category : values()) {
        if (category.displayName.equals(trimmed)) {
            return category;
        }
    }
    return OTHER;
}
```

验收：把 `DrillsTest.day3_categoryFromDisplayName` 里的 `CategoryAnswer` 换成 `Category`，跑绿。

### 6.2 `findAll()`（接口实现第二块）

```java
@Override
public List<Transaction> findAll() {
    return List.copyOf(transactions);
}
```

为什么不是 `return transactions;`：外部拿到内部列表后可以 `add`/`clear`，你的封装就废了；`List.copyOf` 返回不可变副本，测试 `findAll_shouldReturnSnapshot` 就是在验这一点。

### 6.3 给 `Category` 加 `icon()`（switch 表达式）

```java
public String icon() {
    return switch (this) {
        case FOOD -> "🍜";
        case TRANSPORT -> "🚇";
        case SHOPPING -> "🛒";
        case SALARY -> "💰";
        case OTHER -> "📦";
    };
}
```

验收：故意删掉一个 `case` 再编译，看到「switch 表达式未覆盖所有可能值」的报错，就知道穷举检查是真的。然后加回来。

---

## 7. 常见错误与排错

| 现象 | 原因 | 怎么修 |
|---|---|---|
| `Missing @Override annotation` 警告或方法没被调用 | 签名没对上（参数类型/返回值不同） | 加上 `@Override`，让编译器帮你查 |
| `cannot inherit from final ...` | 父类是 `final`（如 `String`） | 换设计，别继承 |
| `default method ... 冲突` | 多个接口有同名 default 方法 | 实现类里显式 `@Override` 并选一个 |
| `IllegalArgumentException: No enum constant` | `valueOf` 传了不存在的名字 | 先 `try/catch` 或用 `Arrays.stream(values())` 匹配 |
| 枚举 `switch` 报「未覆盖」 | 少写了某个枚举值 | 补分支（这就是枚举的价值） |
| `List.copyOf` 后 `add` 抛 `UnsupportedOperationException` | 返回的是不可变列表 | 需要可变就在调用方 `new ArrayList<>(list)` |

---

## 8. 面试怎么问 / 到什么深度

| 问题 | 答到什么程度 |
|---|---|
| 接口和抽象类区别 | 见第 3 节表格，能举例 |
| 接口可以有方法体吗 | JDK 8+ 可以（default/static），JDK 9+ 还可以 private |
| 重写的要求 | 方法名、参数列表相同；返回值协变；不能重写 `final`/`static`/`private` 方法 |
| 多态的实现原理 | 说「方法表 + 运行时根据对象真实类型分派」即可，不用讲虚方法表细节 |
| 枚举为什么安全 | 类加载时初始化、实例固定、`==` 安全、可防反射攻击（构造器私有） |
| 为什么优先组合 | 继承耦合父类实现，组合只依赖接口，易替换易测试 |

**不要做的事**：本周不碰泛型接口、不写自定义注解、不研究 ClassLoader。
