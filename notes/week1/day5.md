# Day 5：集合（List / Set / Map）与排序

> 时长 6h：上午 2.5h 概念，下午 3h 写三个方法 + 复现两个坑，晚上 0.5h 笔记
> 今天是本周最重的一天，也是后端面试问得最多的一天。

---

## 1. 集合全景

```
Collection
├── List          有序、可重复、有下标：ArrayList / LinkedList
├── Set           不重复：HashSet / LinkedHashSet / TreeSet
└── Queue         队列：ArrayDeque / PriorityQueue（第 3 周用）

Map（不属于 Collection）  键值对：HashMap / LinkedHashMap / TreeMap / ConcurrentHashMap
```

选型口诀：

| 需求 | 选 | 理由 |
|---|---|---|
| 按顺序存、频繁按下标读 | `ArrayList` | 数组实现，随机访问 O(1) |
| 频繁在头部插入/删除 | `LinkedList`（但实际项目很少） | 链表，头尾操作 O(1)，随机访问 O(n) |
| 去重 | `HashSet` | 靠 `hashCode` + `equals` |
| 键值查找 | `HashMap` | 平均 O(1) |
| 需要按 key 排序 | `TreeMap` | 红黑树，O(log n) |
| 需要保持插入顺序 | `LinkedHashMap` | 额外链表维护顺序 |

**前端视角的坑**：JS 数组 `unshift` 是 O(n) 但用起来没感觉；Java 的 `ArrayList.add(0, x)` 同样是 O(n)，数据量大时会明显卡。

---

## 2. ArrayList：三个必须知道的事实

1. **默认容量 10，但延迟到第一次 `add` 才分配**（`new ArrayList<>()` 时内部是空数组）。
2. **扩容 1.5 倍**：`newCapacity = oldCapacity + (oldCapacity >> 1)`，然后把老数组 `Arrays.copyOf` 过去。所以频繁添加大量元素时，预估容量 `new ArrayList<>(1000)` 能省掉多次拷贝。
3. **按下标读 O(1)，中间插入/删除 O(n)**（要挪动后面的元素）。

```java
List<String> list = new ArrayList<>();   // 泛型：只能放 String
list.add("a");
list.add(0, "b");        // O(n)
list.get(0);             // O(1)
list.remove("a");        // 按 equals 删除第一个匹配
list.size();
```

---

## 3. 遍历：三种写法 + fail-fast

```java
// 1. 增强 for（最常用）
for (Transaction t : transactions) {
    System.out.println(t);
}

// 2. 下标 for（需要 index 时）
for (int i = 0; i < transactions.size(); i++) { ... }

// 3. forEach（Lambda，Day 6）
transactions.forEach(System.out::println);
```

**坑：遍历时删除**：

```java
for (Transaction t : transactions) {
    if (t.getAmount().signum() < 0) {
        transactions.remove(t);        // ConcurrentModificationException
    }
}
```

增强 for 底层是迭代器，`remove` 会让 `modCount` 变化，下一次 `next()` 抛 `ConcurrentModificationException`（fail-fast）。正确写法：

```java
transactions.removeIf(t -> t.getAmount().signum() < 0);   // 推荐
// 或
Iterator<Transaction> it = transactions.iterator();
while (it.hasNext()) {
    if (it.next().getAmount().signum() < 0) {
        it.remove();
    }
}
```

**到什么深度**：知道 fail-fast 是「检测到并发修改就尽快抛异常」，不是线程安全机制；`ConcurrentHashMap` 的迭代器是弱一致的，不抛。

---

## 4. `Arrays.asList` 与 `List.of` 的坑（面试常问）

```java
List<String> a = Arrays.asList("x", "y");   // 固定大小：能 set，不能 add/remove
a.add("z");                                  // UnsupportedOperationException

List<String> b = List.of("x", "y");          // 完全不可变：set 也不行
b.set(0, "z");                               // UnsupportedOperationException

List<String> c = new ArrayList<>(a);         // 可变副本
c.add("z");                                  // OK
```

`Arrays.asList` 返回的是 `Arrays` 内部的 `ArrayList`（同名但不同类），底层直接引用原数组，所以长度固定。`List.of` 是 JDK 9 引入的真正不可变集合。

---

## 5. HashMap：结构 + 关键数字

```
table（数组，长度 2 的幂，默认 16）
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ 0  │ 1  │ 2  │... │    │    │ 14 │ 15 │
└────┴────┴────┴────┴────┴────┴────┴────┘
         │
         ▼
    链表 → 链表 → ...（长度 ≥ 8 且 table ≥ 64 时转红黑树）
```

四个数字：

| 参数 | 默认值 | 含义 |
|---|---|---|
| 初始容量 | 16 | 2 的幂，便于用位运算取模 |
| 负载因子 | 0.75 | 元素数 > 容量 × 0.75（=12）时扩容 |
| 树化阈值 | 8 | 单桶链表长度到 8 且 table ≥ 64 转红黑树 |
| 反树化阈值 | 6 | 扩容拆分后 ≤ 6 退回链表 |

`put` 的流程（能口述即可）：

1. `hash = (h = key.hashCode()) ^ (h >>> 16)` 扰动，让高位参与定位；
2. `index = (n - 1) & hash` 定位桶；
3. 桶为空 → 直接放；桶非空 → 逐个比较 `hash` 和 `equals`，相同就覆盖 value；
4. 链表追加（JDK 8 是尾插），或树节点插入；
5. `size > threshold` → 扩容 2 倍，元素要么留原位，要么挪到「原位 + oldCap」（JDK 8 的优化，不用重新算 hash）。

**面试怎么问**：HashMap 为什么线程不安全？答：并发 put 可能覆盖、扩容时链表成环（JDK 7 头插法）、size 计数不准。要线程安全用 `ConcurrentHashMap`（第 3 周讲）。

**前端视角的坑**：JS 的 `Map` 用 `===` 比较 key，Java 用 `equals` + `hashCode`。自定义对象做 key 时，不重写 `hashCode` 就会「放进去拿不出来」。

---

## 6. equals 与 hashCode 的契约

规则：**`equals` 为 true 的两个对象，`hashCode` 必须相等**；`hashCode` 相等的对象，`equals` 不一定相等（哈希碰撞）。

```java
class BadKey {
    String id;
    @Override public boolean equals(Object o) { ... }   // 只重写了 equals
    // 没有 hashCode → 默认按地址算，两个「相等」的对象落在不同桶
}
```

复现：`Map<BadKey, String> map`，放一个 key 进去，用「内容相同的新对象」`get`——拿不到。

**什么时候用**：实体类、值对象做 Map key / Set 元素时必须；用 record 就自动帮你生成（day2 提过）。

---

## 7. 排序：Comparable 与 Comparator

```java
// 自然顺序：类实现 Comparable
public class Transaction implements Comparable<Transaction> {
    @Override
    public int compareTo(Transaction other) {
        return this.amount.compareTo(other.amount);
    }
}

// 临时指定顺序：Comparator（更常用，不改类）
list.sort(Comparator.comparing(Transaction::getAmount));              // 升序
list.sort(Comparator.comparing(Transaction::getAmount).reversed());   // 降序
list.sort(Comparator.comparing(Transaction::getDate)
                     .thenComparing(Transaction::getId));             // 多级
```

`compare` 的返回值约定：负数 a 在前、0 相等、正数 a 在后。**不要**写成 `return a - b;`（`int` 可能溢出，`long`/`BigDecimal` 更不行）。

---

## 8. 今天写的三个方法（先 for 循环，Day 6 再改 Stream）

### 8.1 `findByMonth`

```java
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
```

### 8.2 `totalByCategory`

```java
@Override
public Map<Category, BigDecimal> totalByCategory(TransactionType type) {
    Map<Category, BigDecimal> totals = new TreeMap<>();   // 顺序稳定
    for (Transaction transaction : transactions) {
        if (transaction.getType() != type) {
            continue;
        }
        totals.merge(transaction.getCategory(), transaction.getAmount(), BigDecimal::add);
    }
    return totals;
}
```

`Map.merge(key, value, remapping)`：key 不存在就放 value，存在就用 `BigDecimal::add` 合并——比手写 `if (containsKey)` 干净，而且是原子的（`ConcurrentHashMap` 里线程安全）。

### 8.3 `sortedByAmountDesc`

```java
@Override
public List<Transaction> sortedByAmountDesc() {
    List<Transaction> copy = new ArrayList<>(transactions);
    copy.sort(Comparator.comparing(Transaction::getAmount).reversed());
    return copy;
}
```

为什么先 copy：`transactions` 是内部列表，直接 `sort` 会改变插入顺序，影响其他方法的输出。

> 注意：`Collections.emptyList()` 可能返回同一个空实例；`List.copyOf` 在不可变集合上不会重复拷贝——这些是优化细节，知道即可。

---

## 9. 复现两个坑（20 分钟，必须做）

**坑 A：`Arrays.asList` 不能 add**

```java
List<String> list = Arrays.asList("a", "b");
list.add("c");     // UnsupportedOperationException: null
```

改 `new ArrayList<>(Arrays.asList("a", "b"))` 后正常。

**坑 B：只重写 equals 不重写 hashCode**

```java
class Key {
    String id;
    Key(String id) { this.id = id; }
    @Override public boolean equals(Object o) {
        return o instanceof Key k && id.equals(k.id);
    }
    // 故意不写 hashCode
}
Map<Key, String> map = new HashMap<>();
map.put(new Key("1"), "v");
map.get(new Key("1"));     // null
```

加上 `@Override public int hashCode() { return id.hashCode(); }` 后返回 `"v"`。

验收：两条都能复现，并用一句话解释原因，写进笔记。

---

## 10. 常见错误与排错

| 现象 | 原因 | 怎么修 |
|---|---|---|
| `ConcurrentModificationException` | 遍历时直接 `remove` | `removeIf` / `iterator.remove()` |
| `UnsupportedOperationException`（add） | 用了 `Arrays.asList` / `List.of` | `new ArrayList<>(...)` |
| `map.get(key)` 返回 null | key 没重写 `hashCode`，或 key 被修改 | 重写 `hashCode`；不要用可变对象做 key |
| `IllegalArgumentException: Comparison method violates its general contract` | Comparator 不满足传递性 | 用 `Comparator.comparing`，别手写随意的比较逻辑 |
| `IndexOutOfBoundsException` | 下标越界 / 遍历时 `size` 变了 | 检查循环边界 |
| `ClassCastException` | 泛型写了但混入别的类型（原始类型） | 别用裸类型 `List list` |

---

## 11. 面试怎么问 / 到什么深度

| 问题 | 答到什么程度 |
|---|---|
| ArrayList 和 LinkedList 区别 | 底层结构、随机访问、头尾插入、内存开销；补一句「实际项目 90% 用 ArrayList」 |
| ArrayList 扩容机制 | 默认 10（懒加载）、1.5 倍、`Arrays.copyOf` |
| HashMap 原理 | 数组 + 链表/红黑树、hash 扰动、`(n-1)&hash`、0.75 扩容、树化阈值 8 |
| HashMap 为什么线程不安全 | 并发写覆盖、扩容数据错乱、size 不准；替代 `ConcurrentHashMap` |
| `equals` / `hashCode` 契约 | 见第 6 节，必须能复现「只写 equals」的 bug |
| `List.of` 与 `Arrays.asList` | 完全不可变 vs 固定大小 |
| fail-fast 是什么 | 迭代器检测 `modCount` 变化，快速失败，不是线程安全 |

**今天不展开**：`ConcurrentHashMap` 的分段/CAS 细节、`TreeMap` 红黑树旋转、`PriorityQueue`——第 3 周和面试前再补。
