# fullstack-journey

8 年前端（Vue）→ 全栈 + AI Agent 应用开发。这里记我每天真实的学习进度：笔记、能跑的代码、以及踩过的坑。

不是教程合集，也不是"30 天速成"打卡。每一周有一份可验证的验收标准，代码能编译、测试能跑绿，笔记里的结论都来自我自己机器上的真实输出。

## 为什么开这个仓库

我做了 8 年前端，长期用 Vue 做业务需求。2026 年离职后决定往后端和 AI 应用方向走，给自己定了 12–16 周的计划：

- 先把 Java + Spring 这条后端主线打通（不能只停留在"能看懂"）
- 再补 AI Agent 应用开发（检索、工具调用、工作流），不是去卷算法训练
- 每周留下可运行的代码和笔记，作为面试时能拿出来讲的东西

所以这个仓库的提交记录就是我的学习进度，没有为了好看而补的空提交。

## 当前进度

| 周 | 主题 | 状态 |
|---|---|---|
| Week 0 | 环境搭建：JDK 21 / Maven / Git / IDEA / Docker(WSL2) / MySQL 8.4 / Redis 7.4 | 已完成 |
| Week 1 | Java 语言基础：语法、面向对象、异常、集合、Stream、文件 IO | 材料与练习工程已就绪，逐日完成中 |
| Week 2 | 集合与泛型进阶、文件流式处理、日志分析器 | 未开始 |
| Week 3 | 并发与线程池、任务调度器、JVM 基础 | 未开始 |

Week 0 的真实产出（不是"装完就算"）：

- 把 Spring Boot 项目从 H2 切到真实 MySQL 8.4 + Redis 7.4，两个容器均 healthy
- 实测缓存 Cache Aside：`MISS → PUT(TTL 10m) → HIT`，并用空值缓存验证防穿透（`TTL 30s`）
- 实测事务提交与回滚：比较下单前后订单表、购物车、SKU 库存的变化
- 修掉一个真实缺陷：`GET /api/products/abc` 从 500 变 400（`INVALID_PARAMETER`），并补了 MockMvc 测试

Week 1 的产出是 `sandbox/java-basics`：一个 Maven 工程，含四个每日 drill、一个 CLI 记账本、一个日志分析器雏形、一个线程池调度器骨架，20 个 JUnit 5 测试。

## 目录导航

```
notes/
├── week0-environment.md          # Week 0 环境搭建全过程（命令、端口、验证输出、踩坑）
├── week0-database-redis.md       # H2 → MySQL/Redis 的差异、缓存行为、事务实测
├── 一个请求的一生.md              # 一个 GET 请求从 Controller 到数据库的完整链路
├── project-personal-site/
│   └── plan.md                   # 个人网站（首页 AI Agent）的分阶段建设计划，嵌入 16 周节奏
└── week1/
    ├── index.md                  # 本周目标、七天日历、验收清单、自测题（含答案）
    ├── day1.md                   # 基本类型 / 字符串 / == 与 equals / 值传递
    ├── day2.md                   # 类、对象、构造器、封装、POJO
    ├── day3.md                   # 继承、多态、接口、枚举
    ├── day4.md                   # 异常体系与错误处理
    ├── day5.md                   # 集合与排序
    ├── day6.md                   # Lambda / Stream / Optional / BigDecimal / java.time
    └── day7.md                   # 文件 IO、CSV、JUnit 5、本周验收

sandbox/
├── java-basics/                  # Week 1 练习工程（Maven + JDK 21 + JUnit 5）
│   └── src/main/java/com/aya/learning/
│       ├── drills/               # 每日小练习（TODO 骨架 + answers 参考答案）
│       ├── moneybook/            # CLI 记账本（接口 + 实现骨架 + 参考答案 + demo）
│       ├── loganalyzer/          # 日志分析器（Week 2 主线，本周先跑内存版）
│       └── taskrunner/           # 并发调度器骨架（Week 3 主线）
└── start-mall-default.ps1        # 用默认 profile（MySQL + Redis）启动学习用的 mall 项目
```

## 技术栈

- 语言：Java 21、少量 TypeScript/Vue（前端老本行）
- 构建：Maven 3.9.9
- 框架：Spring Boot 3.5.x、Spring Security（JWT）、MyBatis-Plus、Druid
- 存储：MySQL 8.4、Redis 7.4、H2（仅本地降级用）
- 环境：Windows + WSL2 + Docker Desktop
- 测试：JUnit 5、MockMvc

## 怎么跑练习工程

```powershell
cd sandbox/java-basics

# 跑全部测试（20 个）
mvn -B test

# 跑记账本 CLI
java '-Dfile.encoding=UTF-8' -cp target/classes com.aya.learning.moneybook.demo.MoneyBookDemo
```

说明：PowerShell 下 JVM 参数要加引号，否则 `-Dfile.encoding=UTF-8` 会被拆坏（我踩过）。

`src/main/java/.../drills` 和 `moneybook/InMemoryMoneyBookService.java` 里是留给自己的 TODO 骨架；`solution` / `answers` 包是写完之后用来对照的参考答案。测试默认跑参考答案，做完练习后把测试里的实现类换成自己的，能跑绿就说明行为对齐。

## 关于学习用的 mall 项目

`apps/fullstack-mall` 是 https://github.com/2530622506/fullstack-mall 的副本，原作者是 2530622506。我用它来读真实的 Spring Boot 分层、缓存和事务代码：

- **不放进本仓库**：它不是我的代码，原仓库也没有 LICENSE
- 需要时以 GitHub fork 的形式保留原归属和历史（fork 地址见仓库主页链接）
- 我在它上面做的唯一改动是修了一个参数类型错误的 500→400，并在笔记里记录了原因

## 关于提交

- 使用 conventional commits（`feat` / `docs` / `test` / `chore` / `ci`）
- 一次提交只做一件事
- 不伪造提交时间、不生成空提交凑绿格子；没有真实进度就不提交

## License

本仓库中我自己写的笔记和代码采用 MIT 协议，见 [LICENSE](LICENSE)。第三方项目（`fullstack-mall`）不在此协议覆盖范围内，版权归原作者。
