# Day 1：先把「JS 直觉」纠正过来

> 时长 5h：上午 2h 概念 + 敲 jshell，下午 2.5h 练习，晚上 0.5h 笔记
> 今天不写业务代码，只做一件事：让 6 个在 JS 里成立、在 Java 里不成立的直觉变得可验证。

---

## 0. 先让工具链闭环（30 分钟）

Java 不像 JS 直接 `node app.js`：先编译成 `.class`，再由 JVM 执行。

```powershell
cd C:\Develop\private\fullstack-learning\sandbox\java-basics
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:Path="$env:JAVA_HOME\bin;C:\Develop\private\fullstack-learning\tools\apache-maven-3.9.9\bin;"+$env:Path

mvn -q compile
java -cp target/classes com.aya.learning.taskrunner.demo.TaskRunnerDemo
```

看到 5 行输出就说明编译 → 运行闭环通了。三条命令的分工：

| 命令 | 作用 | 前端类比 |
|---|---|---|
| `mvn compile` | 把 `src/main/java` 编译到 `target/classes` | `vite build` 的编译阶段 |
| `mvn test` | 编译 + 跑 `src/test/java` 下的测试 | `vitest run` |
| `java -cp ...` | 启动 JVM 执行某个 `main` | `node dist/index.js` |

再打开交互环境（今天会一直用）：

```powershell
jshell
```

`jshell` 是 JDK 自带的 REPL，等于 Java 版的浏览器控制台。退出用 `/exit`。

---

## 1. 静态类型：变量有类型，而且编译期就检查

```java
int count = 3;
count = "hello";   // 编译直接报错：不兼容的类型
```

JS 里 `let count = 3` 之后随便赋值；Java 变量的类型一旦确定就不能变（除非用 `var` 让编译器推断，但推断出来的类型也是固定的）。

**什么时候用**：所有地方。你写 Spring 代码时，方法签名 `public ApiResponse<ProductDetailResponse> detail(Long id)` 就是靠这套类型系统在编译期挡住一半错误。

**面试怎么问**：「Java 和 JavaScript 的区别？」别答「一个是静态一个是动态」就完了，补一句：静态类型让 IDE 能跳转、重构、在编译期发现参数类型错误，代价是写起来更啰嗦。

**到什么深度就够**：知道 8 种基本类型 + 引用类型的区别，知道 `var` 只能用于局部变量且必须初始化。**不用**背类型转换优先级表。

---

## 2. 基本类型 8 种，没有 `undefined`

| Java | 大小 | 默认值 | 对应 JS |
|---|---|---|---|
| `byte` / `short` / `int` / `long` | 1/2/4/8 字节 | 0 | `number`（整数） |
| `float` / `double` | 4/8 字节 | 0.0 | `number` |
| `char` | 2 字节（UTF-16） | `'\u0000'` | 单字符字符串 |
| `boolean` | JVM 相关 | `false` | `boolean` |

两个关键差异：

1. **没有 `undefined`**。只有 `null`，而且 `null` 只能赋给引用类型，`int x = null` 编译不过。
2. **基本类型不能为 null**，所以后端 DTO 里「这个字段可能没传」通常用包装类 `Integer` / `Long` / `Boolean`，而不是 `int`。

```java
int a = 0;          // 合法，默认 0
Integer b = null;   // 合法，b 没有值
int c = null;       // 编译错误
```

包装类的代价：多一层对象、可能 NPE。**什么时候用包装类**：数据库字段可空、JSON 字段可选、泛型（`List<Integer>` 不能写 `List<int>`）。**什么时候用基本类型**：循环计数、局部计算。

---

## 3. 字符串：不可变 + 常量池

```java
String s = "abc";
s.toUpperCase();          // 返回值被丢弃，s 还是 "abc"
String upper = s.toUpperCase();   // 正确写法
```

`String` 的每个「修改」方法都返回**新对象**。前端里 `str.replace(...)` 也是这样，这点直觉是对的；但 Java 多了「字符串常量池」：

```java
String a = "abc";              // 字面量，进常量池
String b = "abc";              // 复用池里同一个对象
String c = new String("abc");  // 强制新建一个对象，不入池复用
```

所以 `a == b` 是 true，`a == c` 是 **false**。

**什么时候用**：拼接 SQL、拼日志、做 Map 的 key。**面试怎么问**：`String` 为什么不可变 / `String`、`StringBuilder`、`StringBuffer` 的区别。**到什么深度**：能说「不可变 → 可安全共享、可做 key、线程安全；频繁修改用 StringBuilder；StringBuffer 是带 synchronized 的老版本，现在基本不用」。

---

## 4. `==` 与 `equals`：这是今天最重要的一节

| 比较方式 | 比什么 | 什么时候用 |
|---|---|---|
| `==` | 两个变量是否指向**同一个对象**（基本类型比数值） | 比 `int`/`boolean`，或判断是否为 `null` |
| `equals` | 两个对象的**内容**是否相等 | 比 `String`、比业务对象（需自己重写） |

在 jshell 里逐条敲，把结果记到笔记里：

```java
String a = "abc";
String b = "abc";
String c = new String("abc");
a == b            // true
a == c            // false
a.equals(c)       // true
a == null         // false（安全）
a.equals(null)    // false
null.equals(a)    // NullPointerException
```

最后一行就是为什么要写 `Objects.equals(a, b)` 或者 `"abc".equals(x)`（把常量放前面）。

### Integer 缓存（面试高频）

```java
Integer x = 127, y = 127;
x == y        // true   ← 缓存
Integer m = 1000, n = 1000;
m == n        // false  ← 超出缓存，每次都是新对象
m.equals(n)   // true
```

`Integer.valueOf()` 对 **-128 ~ 127** 有缓存（可用 `-XX:AutoBoxCacheMax` 扩大上界）。这就是「明明值相等，`==` 却 false」的来源。

**到什么深度**：知道缓存范围、知道自动装箱/拆箱发生在编译期（`Integer i = 1` 等价于 `Integer.valueOf(1)`）、知道 `null` 的 `Integer` 拆箱会 NPE（`Integer i = null; int j = i;` 直接炸）。

---

## 5. 值传递：Java 只有值传递

```java
static void changeName(Person p) {
    p.name = "李四";      // 改的是同一个对象的字段 → 外部可见
}

static void reassign(Person p) {
    p = new Person("赵六");  // 只改了方法内的局部变量 → 外部不可见
}
```

传对象时，传的是**引用的副本**。所以「改字段生效、重新赋值不生效」两件事同时成立。

**什么时候用**：解释为什么 Service 方法一般返回新对象而不是「改参数」；解释 `List` 传进方法后 `add` 会影响外部（同一个列表对象）。

**面试怎么问**：「Java 是值传递还是引用传递？」标准答法：值传递；对象参数传递的是引用的值（副本）。能举上面两个方法的例子就够了。

---

## 6. 数字：整数除法、溢出、浮点误差

```java
5 / 2          // 2，不是 2.5
5 / 2.0        // 2.5
(int) 2.9      // 2，直接截断，不四舍五入
Integer.MAX_VALUE + 1   // -2147483648，溢出，不报错
0.1 + 0.2      // 0.30000000000000004
```

**什么时候用**：分页计算 `(total + size - 1) / size` 依赖整数除法向上取整；金额永远 `BigDecimal`（Day 6 细讲）。

**到什么深度**：知道整数除法截断、知道溢出不会抛异常（要警惕 `long` 也溢出）、知道 `double` 不能算钱。**不用**背浮点 IEEE 754 的位布局。

---

## 7. 可运行示例：`DrillDay1`

打开 `src/main/java/com/aya/learning/drills/DrillDay1.java`，四个方法都是 `throw new UnsupportedOperationException("TODO ...")`。先跑测试看它红：

```powershell
mvn -B test -Dtest=DrillsTest
```

此时 `DrillDay1` 的方法没被测试调用（测试调的是 `Answers`），所以是绿的——测试现在验的是**参考答案**。你的做法是：实现 `DrillDay1` 后，把 `DrillsTest` 里 `Answers.sameWithOperator` 改成 `DrillDay1.sameWithOperator`，逐个替换、逐个跑。

四个方法的要点：

| 方法 | 你要写的 | 陷阱 |
|---|---|---|
| `sameWithOperator` | `left == right` | 别自作主张改成 equals |
| `sameWithEquals` | `Objects.equals(left, right)` | 直接 `left.equals` 会 NPE |
| `averageOfTwo` | `(a + b) / 2.0` | 写成 `(a+b)/2` 得到 3.0 |
| `boxedSame` | `Integer.valueOf(a) == Integer.valueOf(b)` | 大数会 false |

---

## 8. 手写练习（2.5h，含验收）

**练习 1（必做）**：实现 `DrillDay1` 四个方法，替换测试后 `DrillsTest` 8 个全绿。
验收：`[INFO] Tests run: 8, Failures: 0, Errors: 0`。

**练习 2（必做）**：在 `jshell` 里复现三件事并截图/贴文本到笔记：字符串常量池、Integer 127/1000 差异、`null.equals()` 的 NPE。
验收：三条结论都能用自己的话解释，而不是「教程说这样」。

**练习 3（选做，练 printf）**：新建 `sandbox/java-basics/src/main/java/com/aya/learning/drills/MyBmi.java`，写 `main` 输出：
```
身高 1.75m，体重 70.0kg，BMI = 22.9
```
要求 BMI 保留 1 位小数，用 `String.format("%.1f", ...)`。
验收：`java -cp target/classes com.aya.learning.drills.MyBmi` 输出与上面一行一致（22.9）。

---

## 9. 常见错误与排错

| 现象 | 原因 | 怎么修 |
|---|---|---|
| `错误: 找不到符号` / `cannot find symbol` | 类名/方法名拼错，或没 import | 看报错的行号和符号名；IDEA 里 Alt+Enter |
| `UnsupportedOperationException: TODO ...` | 练习骨架还没实现，这是故意的 | 按 TODO 实现，别去改异常 |
| `java -version` 一片红字（NativeCommandError） | Java 把版本写到 stderr，PowerShell 当成错误 | 用 `java -version 2>&1` 或直接忽略 |
| `mvn : 无法将“mvn”项识别为 cmdlet` | 新窗口没设置 PATH | 重新执行开头的 `$env:Path=...` |
| 中文输出乱码 | 控制台编码与 JVM 默认编码不一致 | `java '-Dfile.encoding=UTF-8' ...`（参数必须加引号） |
| `NullPointerException` 但看不出哪一行 | 链式调用中某个返回 null | 拆成多行；看异常栈第一行 `at ...` |

---

## 10. 今天的笔记模板（晚上 10 分钟）

```
今天验证的 3 个事实：
1. a == new String("a") 是 ___，因为 ___
2. Integer 127 与 1000 的 == 差别是 ___，因为 ___
3. 方法里改字段 / 重新赋值 的差别是 ___，因为 ___

今天踩的坑：___
明天先做：DrillDay2
```
