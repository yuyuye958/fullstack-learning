# 第 0 周环境搭建记录（Windows 11 / PowerShell）

- 记录时间：2026-11-03
- 机器：Windows 11（`10.0`，amd64），用户 `AyaInory`
- 结论：**JDK 21 + Maven + Git + IDEA 已就绪；Docker 引擎已可用（WSL 3.0.3.0）；mall 后端已切换到默认 profile（MySQL 8.4.13 + Redis 7.4.12），常驻端口 8080，`GET /api/health`、`GET /api/products`、`GET /api/products/1` 均 200（curl 实测）。**
- 缓存与事务的实测细节见 [week0-database-redis.md](./week0-database-redis.md)。
- 已修复真实缺陷：`GET /api/products/abc` 从 500 改为 400 `INVALID_PARAMETER`，并有 `ProductControllerTest` 覆盖。
- 遗留：`mvn test` 全量（含 Testcontainers）未跑；幂等重复提交、Redis 故障降级路径未实测。

---

## 1. 目录约定

长期学习目录：

```
C:\Develop\private\fullstack-learning\
├── apps\
│   └── fullstack-mall\        # 从临时目录迁移来的仓库，保留完整 git 历史
├── notes\                     # 学习笔记（本文件所在目录）
├── sandbox\                   # 临时脚本、JWT 密钥、启动日志
└── tools\                     # 手工安装的工具（Maven、MySQL 等）
    └── apache-maven-3.9.9\
```

仓库已不在临时目录 `C:\Users\AyaInory\AppData\Local\Temp\fs-mall-inspect`。

---

## 2. 环境清单（真实版本与路径）

| 组件 | 状态 | 版本 | 安装路径 | 验证命令 | 验证结果 |
|---|---|---|---|---|---|
| Windows | 已有 | 10.0（Windows 11），amd64 | — | `mvn -v` 输出中的 `OS name` | `OS name: "windows 11", version: "10.0", arch: "amd64"` |
| Git | 已有 | 2.34.0.windows.1 | 系统 PATH 中 | `git --version` | `git version 2.34.0.windows.1` |
| JDK | 已安装 | Temurin 21.0.13+9 LTS | `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot` | `& "$env:JAVA_HOME\bin\java.exe" -version` | `openjdk version "21.0.13" 2026-10-20 LTS` / `OpenJDK Runtime Environment Temurin-21.0.13+9 (build 21.0.13+9-LTS)` |
| Maven | 已安装（手工 zip） | 3.9.9 | `C:\Develop\private\fullstack-learning\tools\apache-maven-3.9.9` | `mvn -v` | `Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)`；`Java version: 21.0.13, vendor: Eclipse Adoptium` |
| Docker CLI | 已安装 | 29.9.2（Docker Desktop 4.97.0） | `C:\Program Files\Docker\Docker\resources\bin` | `docker --version` | `Docker version 29.9.2, build a82e528` |
| Docker 引擎 | **可用** | Server 29.9.2；WSL 3.0.3.0，发行版 `docker-desktop` Running | `C:\Program Files\Docker\Docker` | `docker info --format '{{.ServerVersion}}'` | `ServerVersion=29.9.2`；`docker ps` 正常 |
| IntelliJ IDEA Community | 已安装 | 2025.2.6.2 | `C:\Program Files\JetBrains` | `winget list --id JetBrains.IntelliJIDEA.Community` | 已安装；`idea64.exe` 存在 |
| MySQL 8.4 | **运行中（容器）** | 8.4.13 | Docker 容器 `fullstack-mall-mysql` | `docker ps` / `mysql -e "SELECT VERSION()"` | `8.4.13`；端口 `0.0.0.0:3308->3306/tcp`，healthy |
| Redis 7.4 | **运行中（容器）** | 7.4.12 | Docker 容器 `fullstack-mall-redis` | `redis-cli INFO server` | `redis_version:7.4.12`；端口 `6379`，healthy |

### 2.1 JDK 安装方式（已执行）

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --accept-package-agreements --accept-source-agreements
```

关键输出（摘要）：`已成功安装`，MSI 哈希校验通过。

安装后持久化用户级环境变量（未改动系统级，也未卸载任何旧 JDK）：

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME','C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot','User')
# Path(User) 前置 JDK 21 的 bin
```

旧 JDK 抢占检查结果：当前 PATH 中 `java`、`javac` 均解析到 Temurin 21；未发现其他 JDK 目录。

> 注意：`java -version` 把版本写到 stderr，PowerShell 在 `$ErrorActionPreference='Stop'` 下会抛 `NativeCommandError`。这是 PowerShell 的行为，不是 JDK 安装失败。用 `2>&1 | Out-String` 捕获即可。

### 2.2 Maven 安装方式（已执行）

winget 中没有可用的 Maven 包（`winget search --id Apache.Maven --exact` 无结果）。改为下载官方二进制包：

- 下载 `apache-maven-3.9.9-bin.zip`（9,202,456 字节）
- 解压到 `tools\apache-maven-3.9.9`
- 设置 `MAVEN_HOME`（User）并把 `%MAVEN_HOME%\bin` 前置到 `Path`

---

## 3. mall 启动：真实过程与真实输出

### 3.1 项目事实（以代码为准，不以 README 为准）

| 项 | 值 | 来源 |
|---|---|---|
| Java 版本 | **21** | `backend/pom.xml` 的 `<java.version>21</java.version>` |
| Spring Boot | 3.5.16 | `backend/pom.xml` parent |
| 模块 | `contract` + `service`（父工程 `packaging=pom`） | `backend/pom.xml` `<modules>` |
| 默认端口 | 8080（可用 `SERVER_PORT` 覆盖） | `application.yml` `server.port` |
| 默认数据库 | MySQL `localhost:3308/fullstack_mall`，用户 `mall` | `application.yml` `spring.datasource` |
| 默认 Redis | `localhost:6379` | `application.yml` `spring.data.redis` |
| JWT 密钥 | `${JWT_SECRET}`，**无默认值** | `application.yml` `security.jwt.secret` |
| 本地免 Docker 方案 | `local` profile：H2 内存库 + 关闭商品详情缓存 | `application-local.yml` |

> README 写的是 Java 17，与 pom 不一致。**以 pom 为准，必须用 JDK 21。**

### 3.2 启动步骤（已在 `local` profile 下验证通过）

```powershell
# 1) 每个新终端显式设置（因为新进程不会自动继承刚写入的 User 环境变量）
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:MAVEN_HOME='C:\Develop\private\fullstack-learning\tools\apache-maven-3.9.9'
$env:Path="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;"+$env:Path

# 2) JWT_SECRET 必须设置，否则 ${JWT_SECRET} 占位符无法解析
$env:JWT_SECRET=(Get-Content 'C:\Develop\private\fullstack-learning\sandbox\jwt-secret.txt' -Raw).Trim()

# 3) 先把 contract 装进本地仓库（父工程目录执行）
cd C:\Develop\private\fullstack-learning\apps\fullstack-mall\backend
mvn -B -pl contract -am -DskipTests install

# 4) 在 service 模块目录启动（不要在父工程目录直接 spring-boot:run）
cd service
mvn -B spring-boot:run "-Dspring-boot.run.profiles=local"
```

`JWT_SECRET` 生成方式（32 字节随机 + Base64，44 字符，满足 HS256 256 位要求）：

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
Set-Content -Path 'C:\Develop\private\fullstack-learning\sandbox\jwt-secret.txt' -Value $secret -NoNewline
```

### 3.3 真实启动日志（摘要）

```
[INFO] Compiling 76 source files with javac [debug parameters release 21] to target\classes
[INFO] BUILD SUCCESS
[INFO] Total time:  02:25 min

  .   ____          _            __ _ _
 :: Spring Boot ::               (v3.5.16)

Starting FullstackMallApplication using Java 21.0.13 with PID 48512
The following 1 profile is active: "local"
Tomcat initialized with port 8080 (http)
DruidDataSource : {dataSource-1} inited
Tomcat started on port 8080 (http) with context path '/'
Started FullstackMallApplication in 2.271 seconds (process running for 2.457)
```

### 3.4 真实接口验证（`Invoke-WebRequest`）

| 请求 | 状态码 | 响应要点 |
|---|---|---|
| `GET http://localhost:8080/api/health` | **200** | `{"code":"SUCCESS","data":{"status":"UP","application":"fullstack-mall-service",...},"traceId":"6b37d3e2...","timestamp":"..."}` |
| `GET http://localhost:8080/api/products?pageNum=1&pageSize=10` | **200** | `total=2`，`records` 含 `id=4`（深入理解计算机系统）与 `id=1`（九成新 iPhone 15），均 `status=ON_SALE` |
| `GET http://localhost:8080/api/products/1` | **200** | `id=1`，`title=九成新 iPhone 15`，`createdBy=5`，`status=ON_SALE` |
| `GET http://localhost:8080/api/products/999` | **404** | `{"code":"PRODUCT_NOT_FOUND","message":"商品不存在","traceId":"7d7a56e6..."}` |

> 终端里中文显示为乱码（如 `æåä¹å iPhone 15`）是 PowerShell 控制台按 GBK 解码 UTF-8 字节导致，不是服务端编码错误。响应头为 `Content-Type: application/json`（UTF-8），前端按 UTF-8 解码即可正常显示。

### 3.5 常驻启动方式（本次实际采用）

问题：在 AI 工具会话里用 `Start-Process` / 后台 shell 启动 `mvn spring-boot:run`，命令结束时子进程会被会话的 Job Object 回收，端口 8080 随之消失（实测多次）。

解决：用 **Windows 计划任务** 启动，进程树独立于工具会话。

启动脚本：`sandbox\start-mall.ps1`

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:MAVEN_HOME='C:\Develop\private\fullstack-learning\tools\apache-maven-3.9.9'
$env:Path="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;"+$env:Path
$env:JWT_SECRET=(Get-Content 'C:\Develop\private\fullstack-learning\sandbox\jwt-secret.txt' -Raw).Trim()
Set-Location 'C:\Develop\private\fullstack-learning\apps\fullstack-mall\backend\service'
& "$env:MAVEN_HOME\bin\mvn.cmd" -B spring-boot:run '-Dspring-boot.run.profiles=local' 2>&1 |
    Out-File -FilePath 'C:\Develop\private\fullstack-learning\sandbox\server.log' -Encoding utf8
```

创建与启动任务：

```powershell
schtasks.exe /create /tn "mall-backend-local" /tr "powershell.exe -NoProfile -ExecutionPolicy Bypass -File C:\Develop\private\fullstack-learning\sandbox\start-mall.ps1" /sc once /st 23:59 /f
schtasks.exe /run /tn "mall-backend-local"
```

真实输出：

```
SUCCESS: Attempted to run the scheduled task "mall-backend-local".
```

任务状态（实测）：

```
TaskName:      \mall-backend-local
Status:        Running
Logon Mode:    Interactive only
```

> 说明：`Win32_Process.Create`（WMI）方式启动的进程会立即退出、不产出日志，已放弃；计划任务方式验证可用。
> 计划任务依赖当前用户交互式登录（`Interactive only`），注销后服务会停。

### 3.5.1 当前实际使用：默认 profile（MySQL + Redis）

任务 `mall-backend-local` 已指向 `sandbox\start-mall-default.ps1`（不再带 `-Dspring-boot.run.profiles=local`）：

```powershell
schtasks.exe /create /tn "mall-backend-local" /tr "powershell.exe -NoProfile -ExecutionPolicy Bypass -File C:\Develop\private\fullstack-learning\sandbox\start-mall-default.ps1" /sc once /st 23:59 /f
schtasks.exe /run /tn "mall-backend-local"
```

该脚本注入的环境变量（不写入仓库文件）：

```
JWT_SECRET    <- sandbox\jwt-secret.txt
DB_URL        jdbc:mysql://localhost:3308/fullstack_mall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME   mall
DB_PASSWORD   mall_dev_password
REDIS_HOST    localhost
REDIS_PORT    6379
MALL_PRODUCT_CACHE_ENABLED true
```

启动日志（实测）：

```
No active profile set, falling back to 1 default profile: "default"
DruidDataSource : {dataSource-1} inited
Tomcat started on port 8080 (http) with context path '/'
Started FullstackMallApplication in ... seconds
```

日志文件：`sandbox\server-default.log`（local 模式的日志是 `sandbox\server.log`）。

前置条件：`docker compose -f docker-compose.dev.yml up -d` 已让 MySQL/Redis healthy。

### 3.5.2 Docker 镜像拉取失败的处理

首次 `docker compose up -d` 报 `registry-1.docker.io` 连接超时。改用国内镜像源拉取后打本地标签（不改 Docker Desktop 配置）：

```powershell
docker pull docker.m.daocloud.io/library/mysql:8.4
docker tag  docker.m.daocloud.io/library/mysql:8.4 mysql:8.4
docker pull docker.m.daocloud.io/library/redis:7.4-alpine
docker tag  docker.m.daocloud.io/library/redis:7.4-alpine redis:7.4-alpine
docker compose -f docker-compose.dev.yml up -d
```

### 3.6 本次复跑验证（服务已在运行）

进程与端口（实测）：

```
Id        : 32140
Name      : java
StartTime : 2026/11/3 17:59:56

LocalAddress LocalPort OwningProcess
::                8080         32140
```

接口（`curl.exe`，真实状态码与响应体）：

| 请求 | 状态码 | 响应体（节选） |
|---|---|---|
| `GET /api/health` | **200** | `{"code":"SUCCESS","message":"操作成功","data":{"status":"UP","application":"fullstack-mall-service","timestamp":"2026-11-03T10:00:19.032040500Z"},"traceId":"2694508011f748fd843f45805217b9d4",...}` |
| `GET /api/products?pageNum=1&pageSize=10` | **200** | `{"code":"SUCCESS","data":{"pageNum":1,"pageSize":10,"total":2,"pages":1,"records":[{"id":4,"categoryName":"图书音像","title":"深入理解计算机系统","status":"ON_SALE"},{"id":1,"categoryName":"手机数码","title":"九成新 iPhone 15","status":"ON_SALE"}]},...}` |
| `GET /api/products/1` | **200** | `{"code":"SUCCESS","data":{"id":1,"categoryId":1,"categoryName":"手机数码","title":"九成新 iPhone 15","subtitle":"原装配件齐全","description":"自用手机，功能正常，轻微使用痕迹。","status":"ON_SALE","createdBy":5,"createdAt":"2026-07-07T09:00:00","updatedAt":"2026-07-07T09:00:00"},...}` |
| `GET /api/products/999` | **404** | `{"code":"PRODUCT_NOT_FOUND","message":"商品不存在","data":null,"traceId":"276983c27a804c4f8bef2d76d784f2f5",...}` |
| `GET /api/products/3`（`OFF_SHELF`） | **404** | `{"code":"PRODUCT_NOT_FOUND",...}`（库里存在，公开接口不可见） |
| `GET /api/products?pageSize=101` | **400** | `{"code":"VALIDATION_ERROR","message":"请求参数校验失败","data":{"fieldErrors":[{"field":"pageSize","message":"每页数量不能大于 100"}]},...}` |
| `GET /api/products/abc` | **500** | `{"code":"INTERNAL_ERROR","message":"服务器内部错误","data":null,...}` —— **真实缺陷**，见 4.10 |

数据来源（实测）：local profile 不连 MySQL，启动时由 `spring.sql.init` 执行
`service/src/main/resources/db/local-schema.sql`（建 8 张表）与 `db/local-data.sql`（种子数据）。
`local-data.sql` 共 4 条商品：`id=1` `ON_SALE`、`id=2` `DRAFT`、`id=3` `OFF_SHELF`、`id=4` `ON_SALE`，所以公开列表 `total=2`。

### 3.7 关闭服务

```powershell
schtasks.exe /end /tn "mall-backend-local"
Stop-Process -Id 32140        # 若 java 进程仍在
```

或删除任务：`schtasks.exe /delete /tn "mall-backend-local" /f`。

---

## 4. 踩过的坑与解决方案

### 坑 1：README 说 Java 17，pom 说 Java 21
- 现象：按 README 装 JDK 17 会编译失败（`release 21` 不被 JDK 17 支持）。
- 解决：统一用 JDK 21。

### 坑 2：不设置 `JWT_SECRET` 启动失败
- 现象：`application.yml` 里 `security.jwt.secret: ${JWT_SECRET}` 没有默认值，`application-local.yml` 也没有覆盖。
- 解决：启动前设置环境变量，值用随机 Base64（≥32 字节）。

### 坑 3：在父工程目录直接 `mvn spring-boot:run`
- 现象：父工程是 `packaging=pom`，`spring-boot:run` 会作用到 `contract` 模块，且 `service` 依赖的 `contract` 快照可能还没安装。
- 解决：先 `mvn -pl contract -am install`，再进入 `service` 目录执行 `mvn spring-boot:run`。

### 坑 4：新 PowerShell 会话找不到 `java` / `mvn`
- 现象：User 环境变量刚写入，但新进程继承的是 IDE 父进程环境。
- 解决：当前会话显式设置 `$env:JAVA_HOME`、`$env:MAVEN_HOME`、`$env:Path`；重启终端或 IDE 后持久化配置才会生效。

### 坑 5：`java -version` 被 PowerShell 当成错误
- 解决：`$ErrorActionPreference='Continue'`，并 `2>&1 | Out-String`。

### 坑 6：winget 没有 Maven
- 解决：下载官方 `apache-maven-3.9.9-bin.zip` 解压使用。

### 坑 7：Docker 引擎起不来
- 证据：
  - `wsl --status`：提示未安装适用于 Linux 的 Windows 子系统，可用 `wsl.exe --install` 安装。
  - `wsl --install --no-distribution` 退出码 1。
  - `(Get-CimInstance Win32_ComputerSystem).HypervisorPresent` → `False`。
  - `whoami /groups` 中 `BUILTIN\Administrators` 为 “Group used for deny only” → 当前进程无管理员权限。
  - `Get-WindowsOptionalFeature` 报 “The requested operation requires elevation”。
  - `docker info` → `failed to connect ... dockerDesktopLinuxEngine`。
- 结论：Docker Desktop 已装好（4.97.0，服务 `com.docker.service` 存在但 `Stopped`），但缺少 WSL2/Hyper-V 与管理员权限，引擎无法启动。
- **已解决（2026-11-03 晚）**：用户自行安装 WSL 后，实测 `wsl -l -v` 显示 `docker-desktop Running 2`，`wsl --version` 为 3.0.3.0，`docker info` 输出 `ServerVersion=29.9.2`，`docker ps` 正常。

### 坑 8：H2 `local` profile 不依赖 Redis
- `application-local.yml` 把 `mall.cache.product-detail.enabled` 设为 `false`，并关闭 `spring.data.redis` 的实际使用路径，所以 Redis 未启动也能跑通商品接口。
- 代价：看不到 Cache Aside 的 Redis 日志（HIT/MISS/PUT）。要练缓存，需要先把 Redis 跑起来并去掉该开关。
- **已解决**：默认 profile 下 `mall.cache.product-detail.enabled=true`，实测 Redis 生成 `mall:product:detail:v1:{id}`，MISS→PUT→HIT 与空值防穿透均已验证（见 `week0-database-redis.md`）。

### 坑 9：工具会话的后台进程会被回收
- 现象：`Start-Process` 或后台 shell 启动的 `mvn spring-boot:run`，在命令结束后被 Job Object 清理，端口 8080 不再监听（实测多次）。
- 尝试过但失败：`Win32_Process.Create`（WMI）创建进程 → 返回成功但进程立即退出、`server.log` 不生成。
- 已采用：Windows **计划任务**（`schtasks.exe`），进程树独立于工具会话，跨命令验证存活（PID 32140 持续存在，任务 `Status: Running`）。
- 说明：这是工具执行环境的限制，不是 mall 或 Windows 的问题；你在自己终端前台运行同样可以常驻。

### 坑 10：路径参数类型错误返回 500 而不是 400（真实缺陷）
- 现象：`GET /api/products/abc` → HTTP 500 `INTERNAL_ERROR`。
- 根因（服务端日志）：
  ```
  Caused by: java.lang.NumberFormatException: For input string: "abc"
      at java.lang.Long.parseLong(Long.java:709)
      at org.springframework.beans.propertyeditors.CustomNumberEditor.setAsText(CustomNumberEditor.java:115)
      at org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver.convertIfNecessary(...)
  ```
- 原因：`@PathVariable Long id` 转换失败抛 `MethodArgumentTypeMismatchException`，而 `GlobalExceptionHandler` 没有专门处理它，落到 `Exception` 兜底 → 500。
- 影响：前端把非法 id 的请求误判为服务端故障；监控里会多出无意义的 500。
- **已修复（2026-11-03 晚）**：`ApiCode` 新增 `INVALID_PARAMETER`；`GlobalExceptionHandler` 增加 `MethodArgumentTypeMismatchException` handler → 400；新增 `ProductControllerTest#shouldReturnBadRequestWhenPathIdIsNotANumber`（`Tests run: 1, Failures: 0`）。重启后实测 `GET /api/products/abc` → `HTTP 400` + `{"code":"INVALID_PARAMETER","message":"参数 id 格式不正确",...}`。

---

## 5. 第 0 周之后的启动方式（给日常使用）

### 5.1 当前实际使用：默认 profile（MySQL 8.4 + Redis 7.4）

这是现在真正在跑的组合。依赖先行，服务后起：

```powershell
# 1) 起依赖容器（compose 里 MySQL 映射 3308，Redis 6379）
cd C:\Develop\private\fullstack-learning\apps\fullstack-mall
docker compose -f docker-compose.dev.yml up -d
docker ps    # 等到 fullstack-mall-mysql / fullstack-mall-redis 都是 (healthy)

# 2) 起服务（计划任务调用 sandbox\start-mall-default.ps1，独立进程，不随终端退出）
schtasks.exe /run /tn "mall-backend-local"

# 3) 看日志 / 停止
Get-Content 'C:\Develop\private\fullstack-learning\sandbox\server-default.log' -Tail 30
schtasks.exe /end /tn "mall-backend-local"
```

前台方式（自己终端里跑，Ctrl+C 停止，同样走默认 profile）：

```powershell
cd C:\Develop\private\fullstack-learning\apps\fullstack-mall\backend\service
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:Path="$env:JAVA_HOME\bin;C:\Develop\private\fullstack-learning\tools\apache-maven-3.9.9\bin;"+$env:Path
$env:JWT_SECRET=(Get-Content 'C:\Develop\private\fullstack-learning\sandbox\jwt-secret.txt' -Raw).Trim()
$env:DB_URL='jdbc:mysql://localhost:3308/fullstack_mall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
$env:DB_USERNAME='mall'; $env:DB_PASSWORD='mall_dev_password'
$env:REDIS_HOST='localhost'; $env:REDIS_PORT='6379'
$env:MALL_PRODUCT_CACHE_ENABLED='true'
mvn spring-boot:run
```

要点：
- `JWT_SECRET` 只从 `sandbox\jwt-secret.txt` 读进环境变量，仓库里 `application.yml` 写的是 `${JWT_SECRET}`，没有默认值也没有硬编码。
- 带 `-Dspring-boot.run.profiles=local` 才是 H2；不带 profile 就是真实 MySQL + Redis 且缓存开启。
- `sandbox\start-mall-default.ps1` 已把这几个环境变量固化，计划任务直接调用它。

### 5.2 H2 备选（`local` profile）

H2 方案只作为「Docker 不可用时的降级路径」保留：

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

它与默认 profile 的差异（schema 来源、SQL 方言、缓存开关等）见 `week0-database-redis.md` 第 4 节。

---

## 6. Docker 前置条件（本次已由用户完成，无需再操作）

原计划需要以管理员身份执行 `wsl --install` 并重启电脑。**用户已自行完成 WSL 安装**，实测状态：

- `wsl -l -v` → `docker-desktop  Running  2`
- `wsl --version` → `WSL 版本： 3.0.3.0`
- `docker info` → `Server Version: 29.9.2`
- `docker ps` → 正常列出两个容器

所以第 6 节不再是阻塞项。之后若要重建环境，只需：

```powershell
cd C:\Develop\private\fullstack-learning\apps\fullstack-mall
docker compose -f docker-compose.dev.yml up -d
```

判断标准：`docker ps` 中 `fullstack-mall-mysql`（`0.0.0.0:3308->3306/tcp`）与 `fullstack-mall-redis`（`0.0.0.0:6379->6379/tcp`）都是 `Up ... (healthy)`。

> 国内网络下 Docker Hub 可能拉不动镜像，改用 `docker.m.daocloud.io` 前缀拉取再 `docker tag`，具体命令见 `week0-database-redis.md` 第 2 节。

---

## 7. 下一步待办

- [x] Docker 引擎可用（WSL `docker-desktop Running 2`，`Server Version: 29.9.2`）。
- [x] 用 `docker compose -f docker-compose.dev.yml up -d` 起 MySQL 8.4.13（3308）+ Redis 7.4.12（6379），两个容器 healthy。
- [x] MySQL 初始化走 `sql/01_schema.sql` + `sql/02_seed.sql`（compose 挂载 `./sql:/docker-entrypoint-initdb.d`），8 张表、seed 数据已导入。
- [x] 默认 profile（真实 MySQL + Redis）启动成功，`/api/health`、`/api/products`、`/api/products/1` 均 200。
- [x] 商品详情缓存开启（`MALL_PRODUCT_CACHE_ENABLED=true`），实测 `MISS → PUT ttl=PT10M → HIT`，空值 `PUT_NULL ttl=PT30S`。
- [x] 修 `GlobalExceptionHandler` 的 `MethodArgumentTypeMismatchException` → 400（`INVALID_PARAMETER`），并补 `ProductControllerTest`。
- [x] 安装 IntelliJ IDEA Community（winget 成功，版本 2025.2.6.2，`idea64.exe` 已存在）。
- [ ] `mvn test` 全量跑一遍（含 Testcontainers 用例）；本次只单跑了 `ProductControllerTest`。
- [ ] 验证幂等重复提交（同一 `Idempotency-Key` 两次）与 Redis 故障时的 `DEGRADED` 降级路径 —— **未验证**。
- [x] `tools\mysql-8.4.7-winx64.zip`（260,872,794 字节）未使用，已删除（2026-11-03，Docker 方案已跑通，不再需要本地 ZIP）。
- [ ] 进入第 1 周：Java 语法基础（不在本次范围内）。
