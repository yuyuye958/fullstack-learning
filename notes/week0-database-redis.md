# 从 H2 切到真实 MySQL + Redis：实测记录

- 记录时间：2026-11-03
- 目的：把 mall 后端从 `local`（H2 内存库、缓存关闭）切到默认 profile（MySQL 8.4 + Redis 7.4、缓存开启），并观察缓存与事务的真实行为。
- 结论：**默认 profile 已跑通；三个 GET 接口 200；缓存 MISS→PUT→HIT 与空值防穿透均实测；事务提交与回滚均实测；`/api/products/abc` 已从 500 修成 400 并有测试。**

---

## 1. 前置：WSL 与 Docker 的真实状态

```
wsl -l -v
  NAME              STATE      VERSION
* docker-desktop    Running    2

wsl --version
  WSL version: 3.0.3.0
  Kernel version: 6.18.9.1-1

docker info --format 'ServerVersion={{.ServerVersion}}'
  ServerVersion=29.9.2

Get-Service com.docker.service
  Status: Running
```

Docker Desktop 之前因 WSL 未安装而无引擎；用户自行安装 WSL 后引擎可用，无需重装。

---

## 2. 拉镜像：Docker Hub 不可达，改用国内镜像源

直接 `docker compose up -d` 失败（真实错误）：

```
failed to resolve source metadata for docker.io/library/mysql:8.4:
failed to do request: Head "https://registry-1.docker.io/v2/library/mysql/manifests/8.4":
dial tcp 202.160.130.66:443: connectex: A connection attempt failed ...
```

连通性探测结果：

| 源 | 结果 |
|---|---|
| `registry-1.docker.io` | 连接超时 |
| `docker.m.daocloud.io` | `401`（需要 token，属正常可达） |
| `dockerproxy.com` | 连接失败 |
| `hub-mirror.c.163.com` | 连接失败 |

系统代理 `ProxyServer=127.0.0.1:7897` 但 `ProxyEnable=0`，Docker Desktop 也未配置代理。

采用的做法（不改 Docker Desktop 配置，只给镜像打本地标签）：

```powershell
docker pull docker.m.daocloud.io/library/redis:7.4-alpine
docker tag  docker.m.daocloud.io/library/redis:7.4-alpine redis:7.4-alpine

docker pull docker.m.daocloud.io/library/mysql:8.4
docker tag  docker.m.daocloud.io/library/mysql:8.4 mysql:8.4

docker compose -f docker-compose.dev.yml up -d
```

---

## 3. 容器、端口与初始化

`docker ps` 实测：

```
fullstack-mall-redis  redis:7.4-alpine  Up (healthy)  0.0.0.0:6379->6379/tcp
fullstack-mall-mysql  mysql:8.4         Up (healthy)  0.0.0.0:3308->3306/tcp
```

- 宿主机 **3308 → 容器 3306**：项目默认连接串就是 3308，避开了本机可能存在的 3306。
- 版本：MySQL **8.4.13**，Redis **7.4.12**。
- 数据库字符集：`utf8mb4` / `utf8mb4_0900_ai_ci`。

### 初始化脚本

`docker-compose.dev.yml` 把仓库的 `sql/` 目录挂到容器的 `/docker-entrypoint-initdb.d`，MySQL 首次启动按文件名顺序执行：

| 脚本 | 作用 | 结果 |
|---|---|---|
| `sql/01_schema.sql` | 建库建表 | 8 张表 |
| `sql/02_seed.sql` | 种子数据 | category 3、product 4、product_sku 4、user 5、cart_item 2、order 0、order_item 0、payment 0 |

8 张表实测：

```
mall_cart_item  mall_category  mall_order  mall_order_item
mall_payment    mall_product   mall_product_sku  mall_user
```

商品状态分布（实测）：

```
1  ON_SALE    九成新 iPhone 15
2  DRAFT      Nintendo Switch OLED
3  OFF_SHELF  Java 核心技术卷 I
4  ON_SALE    深入理解计算机系统
```

> 与 H2 的种子数据一致：4 条商品、2 条 `ON_SALE`，所以公开列表 `total=2` 两种库都对得上。
> 只要 volume 不被删除，初始化只执行一次；容器重启不会重跑脚本，也不会重置数据。

---

## 4. H2 与真实 MySQL 的差异（实测与代码对比）

| 维度 | H2（`local` profile） | MySQL 8.4（默认 profile） | 观察方式 |
|---|---|---|---|
| 连接串 | `jdbc:h2:mem:fullstack_mall;MODE=MySQL;DB_CLOSE_DELAY=-1` | `jdbc:mysql://localhost:3308/fullstack_mall?...` | `application-local.yml` vs `application.yml` |
| 驱动 | `org.h2.Driver` | `com.mysql.cj.jdbc.Driver` | 同上 |
| 建表/灌数据 | `spring.sql.init` 每次启动执行 `db/local-schema.sql` + `db/local-data.sql` | 容器 `docker-entrypoint-initdb.d` 只在**首次**初始化执行 `sql/01_schema.sql` + `sql/02_seed.sql` | 启动日志；`SHOW TABLES` |
| 数据持久性 | 内存库，进程重启即重置 | 存在 Docker volume，重启保留 | 重启服务后 `SELECT COUNT(*)` |
| 中文 | 无编码问题（内存库） | 依赖 `characterEncoding=utf8` + `utf8mb4`，否则中文变 `???` | `mysql --default-character-set=utf8mb4` 前出现乱码 |
| 认证 | 无 | MySQL 8.4 默认 `caching_sha2_password`，连接串需 `allowPublicKeyRetrieval=true`（已在 `application.yml` 中） | 连接串内容 |
| 缓存 | `mall.cache.product-detail.enabled=false`，`lookup` 直接返回 `BYPASS` | `enabled=true`，走 Redis | `application-local.yml#L23-L27` |
| 表名 | `mall_product` 等 8 张 | 同名（`01_schema.sql` 与 `local-schema.sql` 字段对齐） | `SHOW TABLES` |

**实测差异中最容易踩的一点**：H2 下改数据重启就没了；MySQL 下改数据会一直留着。我为了构造回滚场景临时改过 `available_stock`，最后手动改回 8，否则它会一直影响后续测试。

**另一处差异**：`mysql` 命令行客户端在 Windows 控制台默认不是 utf8mb4，直接 `SELECT title` 会显示 `??? iPhone 15`；加 `--default-character-set=utf8mb4` 才正常。**这是客户端显示问题，不是库里数据坏了**——API 返回的中文一直是正确的。

---

## 5. 缓存真实行为观察（Cache Aside）

### 5.1 首次读：MISS → 回源 → PUT

命令：

```powershell
docker exec fullstack-mall-redis redis-cli DEL 'mall:product:detail:v1:1'
curl.exe -s -o NUL -w "HTTP=%{http_code}" http://localhost:8080/api/products/1
```

Redis 实测：

```
KEYS mall:product:detail:*
  mall:product:detail:v1:1
TTL mall:product:detail:v1:1
  598
```

- 第一次请求：缓存没有 → `MISS` → 查 MySQL → 回填。
- TTL 598 秒 ≈ 配置的 10 分钟（`PT10M`），说明 `setEx` 用的是值 TTL。
- key 结构 `mall:product:detail:v1:{productId}` 与 `ProductDetailCacheService` 的 `KEY_PREFIX + version + id` 一致。

服务端日志（`sandbox\server-default.log`，2026-11-03 19:38:49 那次实测）：

```
c.e.f.s.p.c.ProductDetailCacheService : product_detail_cache event=MISS productId=1
c.e.f.s.p.c.ProductDetailCacheService : product_detail_cache event=PUT productId=1 ttl=PT10M
c.e.f.s.p.c.ProductDetailCacheService : product_detail_cache event=HIT productId=1
```

（复测时 `DEL` 后第一次读 TTL 直接是 `600`；最初那次读到 `598`，差 2 秒是命令间隔。）

### 5.2 第二次读：HIT

同一商品再请求一次，状态码 200，响应字段与第一次完全一致，Redis TTL 继续递减，MySQL 不再被查询（日志里没有新的查询记录）。

### 5.3 空值防穿透：MISS → PUT_NULL → NULL_HIT

```powershell
docker exec fullstack-mall-redis redis-cli DEL 'mall:product:detail:v1:999'
curl.exe -s http://localhost:8080/api/products/999      # 第 1 次
docker exec fullstack-mall-redis redis-cli TTL 'mall:product:detail:v1:999'
docker exec fullstack-mall-redis redis-cli GET 'mall:product:detail:v1:999'
curl.exe -s http://localhost:8080/api/products/999      # 第 2 次
```

实测：

```
KEYS  -> mall:product:detail:v1:999
TTL   -> 29
GET   -> {"__NULL__":"PRODUCT_NOT_FOUND"}    （空值标记 + 业务码）
```

- 第 1 次：MySQL 查不到 → 404 → 写入空值标记（TTL 30 秒，`PT30S`）。
- 第 2 次：读到空值标记 → 直接抛 `PRODUCT_NOT_FOUND`，**不再查库**。
- 这就是防穿透：对不存在的 id 反复请求，只有第一次打到 MySQL。

对应日志（同一份 `server-default.log`）：

```
product_detail_cache event=MISS productId=999
product_detail_cache event=PUT_NULL productId=999 code=PRODUCT_NOT_FOUND ttl=PT30S
```

### 5.4 与 H2 模式的对比（关键差异）

H2 模式下 `lookup` 返回 `BYPASS`，Redis 里永远没有 key，`KEYS` 为空。切到默认 profile 后才有上面这些 key。
**这也意味着：H2 阶段看不到缓存相关 bug，必须先接 Redis 才能验证 Cache Aside。**

---

## 6. 事务的真实行为

验证对象：`OrderCreateTransactionService#createOrder`（`@Transactional(rollbackFor = Exception.class)`）。
关键代码顺序：先 `save(order)` 插入订单主表，**再** `skuDbService.lockStock(...)` 条件更新库存。

### 6.1 提交：三张表一致变更

登录 `xiaoming / Mall123456`，购物车已有 1 项（sku 1 × 2），下单：

```
POST /api/orders
Authorization: Bearer <token>
Idempotency-Key: order-test-001

HTTP=201
{"code":"SUCCESS","data":{"id":1,"orderNo":"OA9DC...","status":"PENDING_PAYMENT","totalAmount":9198.00,...}}
```

数据库前后对比（实测）：

| 指标 | 下单前 | 下单后 |
|---|---|---|
| `mall_order` 行数 | 0 | **1** |
| `mall_cart_item`（user 1） | 1 | **0** |
| `sku 1 available_stock` | 10 | **8** |
| `sku 1 locked_stock` | 0 | **2** |

一次调用里：订单插入 + 库存从可售转锁定 + 购物车清空，全部生效。这就是事务提交的可见结果。

### 6.2 回滚：订单已插入，但最终没有留下

构造方式（模拟并发抢库存）：

1. `available_stock` 临时设为 3，加购 2 件 → 加购成功（加购时只校验 `quantity <= available`）。
2. 把 `available_stock` 改成 1（模拟别的请求先抢走库存）。
3. 下单。

实测响应：

```
POST /api/orders   Idempotency-Key: order-test-003
HTTP=409
{"code":"ORDER_INSUFFICIENT_STOCK","message":"订单商品库存不足",...}
```

数据库前后对比（实测）：

| 指标 | 下单前 | 下单后 |
|---|---|---|
| `mall_order` 行数 | 1 | **1** |
| `mall_cart_item`（user 1） | 1 | **1** |
| `sku 1 available_stock` | 1 | **1** |
| `sku 1 locked_stock` | 2 | **2** |

**关键观察**：代码里订单是在 `lockStock` 之前 `save` 的，也就是说中间确实执行过 INSERT；但最终 `mall_order` 行数没变，购物车没被删，库存没被改。这说明抛出的 `BusinessException` 触发了事务回滚，把已经执行的 INSERT 撤销了。

### 6.3 幂等（顺带观察）

`POST /api/orders` 需要 `Idempotency-Key`，格式 `8~64` 位。同一个 key 重复提交不会新建订单（仓库自带并发测试 `OrderMySqlContainerTest` 覆盖了这条）。本次只验证了正常路径与库存不足路径，**幂等重复提交未单独实测**。

### 6.4 清理

测试造成的临时数据已恢复：

```
UPDATE mall_product_sku SET available_stock=8 WHERE id=1;   -- 回到 8（locked 2 对应订单 1）
DELETE FROM mall_cart_item WHERE id=3;                      -- 删除测试加购项
```

订单 `id=1` 保留（它是真实下单产物，与 `locked_stock=2` 自洽）。

---

## 7. 缺陷修复：`/api/products/abc` 从 500 到 400

### 7.1 修复前

```
GET /api/products/abc  ->  HTTP 500
{"code":"INTERNAL_ERROR","message":"服务器内部错误","data":null,...}
```

服务端日志：

```
Caused by: java.lang.NumberFormatException: For input string: "abc"
    at java.lang.Long.parseLong(Long.java:709)
    at org.springframework.beans.propertyeditors.CustomNumberEditor.setAsText(...)
    at ...AbstractNamedValueMethodArgumentResolver.convertIfNecessary(...)
```

### 7.2 改动（最小）

1. `contract/.../common/ApiCode.java`：新增枚举值

   ```java
   INVALID_PARAMETER("请求参数格式不正确"),
   ```

2. `service/.../common/exception/GlobalExceptionHandler.java`：新增 handler，把类型转换失败映射为 400

   ```java
   @ExceptionHandler(MethodArgumentTypeMismatchException.class)
   public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
           MethodArgumentTypeMismatchException exception,
           HttpServletRequest request
   ) {
       ApiResponse<Void> response = ApiResponse.error(
               ApiCode.INVALID_PARAMETER,
               "参数 " + exception.getName() + " 格式不正确",
               null,
               TraceIdContext.get(request)
       );
       return ResponseEntity.badRequest().body(response);
   }
   ```

3. 新增测试 `service/src/test/java/com/example/fullstackmall/service/product/ProductControllerTest.java`：

   ```java
   @SpringBootTest(classes = FullstackMallApplication.class)
   @AutoConfigureMockMvc
   class ProductControllerTest {
       @Resource private MockMvc mockMvc;

       @Test
       void shouldReturnBadRequestWhenPathIdIsNotANumber() throws Exception {
           mockMvc.perform(get("/api/products/abc"))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                   .andExpect(jsonPath("$.traceId").isNotEmpty());
       }
   }
   ```

### 7.3 验证

测试（实测）：

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

重启服务后实测：

```
GET /api/products/abc  ->  HTTP 400
{"code":"INVALID_PARAMETER","message":"参数 id 格式不正确","data":null,"traceId":"d7d6072c591d406c98eccef49dc21104","timestamp":"2026-11-03T11:33:50.481574Z"}
```

---

## 8. 接入命令汇总（可直接复用）

```powershell
# 1) 容器
cd C:\Users\AyaInory\fullstack-learning\apps\fullstack-mall
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml ps

# 2) 验证数据
docker exec fullstack-mall-mysql mysql -umall -pmall_dev_password --default-character-set=utf8mb4 -N -e "SHOW TABLES FROM fullstack_mall;"
docker exec fullstack-mall-redis redis-cli KEYS 'mall:product:detail:*'

# 3) 默认 profile 启动（脚本 sandbox\start-mall-default.ps1，由计划任务 mall-backend-local 调用）
schtasks.exe /run  /tn "mall-backend-local"
schtasks.exe /end  /tn "mall-backend-local"
Get-Content C:\Users\AyaInory\fullstack-learning\sandbox\server-default.log -Tail 20

# 4) 日志位置
#   sandbox\server-default.log   （默认 profile）
#   sandbox\server.log           （local/H2 profile，已停用）
```

默认 profile 的环境变量（写在 `start-mall-default.ps1`，不写进仓库）：

```
JWT_SECRET       <- sandbox\jwt-secret.txt（44 字符 Base64）
DB_URL           jdbc:mysql://localhost:3308/fullstack_mall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME      mall
DB_PASSWORD      mall_dev_password
REDIS_HOST       localhost
REDIS_PORT       6379
MALL_PRODUCT_CACHE_ENABLED true
```

---

## 9. 还没验证的

- 幂等重复提交（同 `Idempotency-Key` 两次）的实际响应：**未验证**
- Redis 故障时 `DEGRADED` fail-open 路径：**未验证**（需要停 Redis 再发请求）
- `mvn test` 全量（含 Testcontainers 用例）：**未跑**，只跑了新增的 `ProductControllerTest`
- 缓存与数据库的一致性延迟（更新商品后缓存是否失效）：**未验证**
- 容器重启后数据是否保留：**未验证**（依赖 volume，理论保留）
