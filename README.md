# 项目说明

## 快速开始
1. 进入环境目录并启动 Docker 依赖：
```bash
cd docs/dev-ops/environment
docker compose -f docker-compose-environment.yml -p environment up -d
```
docker-compose会自根据docs\dev-ops\environment\mysql\sql\lost_and_found_init.sql自动创建名为Lost_And_Found的数据库。。。。。.
2. 本地数据库端口使用 `13307`（MySQL 端口映射）。
3. 应用启动后即可连接本地数据库。


## 1. 配置文件
- `src/main/resources/application.yml`：公共配置（应用名、JWT、MyBatis‑Plus、日志、启用环境）
- `src/main/resources/application-dev.yml`：开发环境（数据库、Redis、连接池）
- `src/main/resources/application-prod.yml`：生产环境（数据库、Redis、连接池）

当前默认启用：`dev`（在 `application.yml` 中配置 `spring.profiles.active=dev`）。

### 邮件配置（注册验证码）
- 配置位置：`application-dev.yml` / `application-prod.yml`
- 关键字段：
  - `spring.mail.host`
  - `spring.mail.port`
  - `spring.mail.username`
  - `spring.mail.password`
  - `app.mail.from`
  - `app.mail.test-to`（可选，用于本地邮件发送测试）
  - `app.jwt.refresh-expiration-ms`（refresh token 有效期）
- 邮件模板：
  - 标题：`注册验证码`
  - 内容：`您的注册验证码为：{code}，有效期 90 秒。如非本人操作请忽略。`


---

## 2. 枚举定义（common/enums）
- `common/enums/ResponseCode`：统一响应码
  - 字段：`code`（错误码）、`info`（错误信息）
- `common/enums/item/ItemType`：物品类型
  - `LOST(0, "挂失")`
  - `FOUND(1, "招领")`
- `common/enums/item/ItemStatus`：物品状态
  - `ACTIVE(0, "有效")`
  - `CLOSED(1, "结束")`
- `common/enums/user/UserStatus`：用户状态
  - `NORMAL(0, "正常")`
  - `BANNED(1, "封禁")`
  - `DEACTIVATED(2, "注销")`

---

## 3. 常量与异常
- `common/constant/Constants`：通用常量、Redis Key 规则
  - 例如：`Constants.RedisKey.ITEM_DETAIL`
- `common/exception/AppException`：业务异常
  - 构造参数：`code`（异常码）、`message`（异常信息）、`cause`（异常原因）
  - 用途：在业务层抛出，交由全局异常处理统一返回
- `common/exception/GlobalExceptionHandler`：全局异常处理
  - `handleAppException(AppException e)`：处理业务异常
  - `handleValidation(Exception e)`：处理参数校验异常
  - `handleException(Exception e)`：处理未捕获异常
- `common/result/Result`：统一返回体
  - 字段：`code`、`info`、`data`
  - 方法：
    - `success(T data)`：成功返回，参数 `data` 为业务数据
    - `fail(ResponseCode code)`：失败返回，参数为枚举错误码
    - `fail(String code, String info)`：失败返回，自定义错误码与信息

---

## 4. 工具类（common/utils）

### 4.1 加密（BCrypt）
- 位置：`common/utils/security/encrypt/BCryptUtils`
- 方法：
  - `hash(String rawPassword)`
    - 参数：`rawPassword` 明文密码
    - 功能：生成不可逆哈希
    - 返回：哈希后的密码
  - `matches(String rawPassword, String hashedPassword)`
    - 参数：明文 + 哈希
    - 功能：校验是否匹配
    - 返回：`true/false`

### 4.2 JWT
- 位置：`common/utils/security/jwt/JwtUtil`
- 方法：
  - `generateToken(String email)`
    - 参数：`email` 用户邮箱
    - 功能：生成 JWT（subject 使用邮箱）
    - 返回：Token 字符串
  - `getEmail(String token)`
    - 参数：`token` JWT
    - 功能：解析邮箱
    - 返回：邮箱
  - `isTokenValid(String token)`
    - 参数：`token` JWT
    - 功能：校验 Token 有效性
    - 返回：`true/false`
  - `getExpirationMs()`
    - 功能：获取过期时间（毫秒）
    - 返回：过期时间

示例（登录签发 + 鉴权校验 + 失效处理）：
```java
// 1) 登录成功后签发 Token（用邮箱作为 subject）
String email = "test@whut.edu.cn";
String token = jwtUtil.generateToken(email);

// 2) 前端携带 Token 访问接口（示意）
// Authorization: Bearer <token>

// 3) 服务端校验 Token
if (!jwtUtil.isTokenValid(token)) {
    // 失效/过期，提示前端重新登录
    return Result.fail("401", "Token 已失效，请重新登录");
}

// 4) 解析邮箱并做权限判断
String loginEmail = jwtUtil.getEmail(token);
// 例如：根据邮箱查用户，再判断是否被封禁
// if (user.getStatus() == UserStatus.BANNED.getCode()) return Result.fail(...)
```

## 4.4 认证与注册接口（Swagger/REST）

### 发送注册验证码
`POST /api/auth/register/code`
- 入参：
  - `email`
- 说明：
  - 验证码 4 位
  - 有效期 90 秒
  - 同一邮箱 60 秒内仅允许发送一次
- 可能返回错误码：
  - `USR_003`：邮箱已存在
  - `USR_006`：验证码发送过于频繁
  - `MAIL_001`：邮箱配置缺失或无效
  - `MAIL_002`：邮件发送失败

### 注册
`POST /api/auth/register`（等价于 `POST /api/users`）
- 入参：
  - `email`
  - `password`
  - `confirmPassword`
  - `code`（邮箱验证码）
  - `nickname`（可选）
- 说明：
  - 注册成功不自动登录、不返回 token
- 可能返回错误码：
  - `USR_003`：邮箱已存在
  - `USR_004`：邮箱验证码无效
  - `USR_005`：邮箱验证码已过期

### 登录
`POST /api/auth/login`
- 入参：
  - `email`
  - `password`
- 返回：
  - `token`
  - `expiresIn`（毫秒，来源于配置 `app.jwt.expiration-ms`）
  - `refreshToken`
  - `refreshExpiresIn`（毫秒，来源于配置 `app.jwt.refresh-expiration-ms`）
- 说明：
  - JWT 的过期时间写入 token 的 `exp` 字段
- 可能返回错误码：
  - `USR_001`：用户不存在
  - `USR_007`：邮箱或密码错误
  - `USR_008`：登录失败次数过多，请5分钟后再试

## 5. 当前用户接口（推荐）
前端只需要保存 token/refreshToken，无需传 userId。

- `GET /api/users/me` 获取当前用户信息  
- `PUT /api/users/me/password` 修改当前用户密码  
- `PUT /api/users/me/nickname` 修改当前用户昵称  
- `DELETE /api/users/me` 注销当前用户

### 刷新
`POST /api/auth/refresh`
- 入参：
  - `refreshToken`
- 返回：
  - `token`
  - `expiresIn`
  - `refreshToken`（已旋转）
  - `refreshExpiresIn`
- 可能返回错误码：
  - `USR_009`：Refresh token 无效

### 退出
`POST /api/auth/logout`
- 入参：
  - `refreshToken`
- 说明：
  - 删除 refresh token，使其无法继续刷新
- 可能返回错误码：
  - `USR_009`：Refresh token 无效

#### 刷新策略说明（前端）
**主动刷新（推荐）**
- 登录后记录过期时间点：`expireAt = now + expiresIn`
- 提前刷新（例如提前 5 分钟）：`refreshAt = expireAt - 5 * 60 * 1000`
- 到点调用 `/api/auth/refresh` 获取新 token

**被动刷新（兜底）**
- 业务请求返回“未登录/过期”时触发刷新
- 前端调用 `/api/auth/refresh`，成功后重试原请求

**实践建议**
- 主动刷新 + 被动刷新结合，体验更平滑

### 4.3 本地锁（防缓存击穿）
- 位置：`common/utils/lock/LocalKeyLock`
- 方法：
  - `getLock(String key)`
    - 参数：`key` 业务键
    - 功能：获取/创建本地锁
    - 返回：`ReentrantLock`
  - `unlock(String key, ReentrantLock lock)`
    - 参数：`key`、`lock`
    - 功能：释放锁并清理缓存
  - `withLock(String key, Supplier<T> supplier)`
    - 参数：`key`、`supplier` 执行逻辑
    - 功能：自动加锁/解锁，返回执行结果

示例（缓存击穿场景）：
```java
String cacheKey = "item:detail:" + itemId;
Object cached = redisService.getValue(cacheKey);
if (cached != null) {
    return Result.success(cached);
}

// 缓存未命中时，用本地锁保护“回源到 DB”的逻辑
Object data = localKeyLock.withLock(cacheKey, () -> {
    Object secondCheck = redisService.getValue(cacheKey);
    if (secondCheck != null) {
        return secondCheck;
    }
    // 回源查询数据库
    Object dbData = itemMapper.selectById(itemId);
    redisService.setValue(cacheKey, dbData);
    return dbData;
});
return Result.success(data);
```

### 4.4 分页工具
- 位置：`common/utils/page/PageUtils`
- 方法：
  - `toPageResult(IPage<T> page)`
    - 参数：MyBatis‑Plus `IPage<T>`
    - 功能：转换为统一分页 VO
    - 返回：`PageResultVO<T>`

示例（返回分页列表）：
```java
public Result<PageResultVO<Item>> list(PageQueryDTO query) {
    Page<Item> page = new Page<>(query.getPageNo(), query.getPageSize());
    IPage<Item> result = itemMapper.selectPage(page, null);
    return Result.success(PageUtils.toPageResult(result));
}
```

---

## 5. Redis 扩展能力（common/utils/bloom）
- 布隆过滤器：`common/utils/bloom/RedisBloomFilter`
  - `add(String value)`：写入位图
  - `mightContain(String value)`：判断可能存在
- 工厂：`common/utils/bloom/factory/BloomFilterFactory`
  - `getBloomFilter(String key, long expectedInsertions, double fpp)`：按 key 复用实例

示例（防止缓存穿透）：
```java
// 1) 系统启动或数据初始化时，提前灌入已有 itemId
RedisBloomFilter bloom = bloomFilterFactory.getBloomFilter(
        Constants.RedisKey.ITEM_BLOOM, 100000, 0.01);
bloom.add("1001");
bloom.add("1002");

// 2) 查询时先用布隆过滤器判断
String itemId = "9999";
if (!bloom.mightContain(itemId)) {
    // 肯定不存在，直接返回，避免打到数据库
    return Result.fail("404", "物品不存在");
}
// 可能存在，再查缓存/数据库
```

---

## 6. DTO / VO / Entity
- DTO：`model/dto`（如 `PageQueryDTO`）
  - `pageNo`：当前页（从 1 开始）
  - `pageSize`：每页大小
- VO：`model/vo`（如 `PageResultVO`）
  - `pageNo`、`pageSize`、`total`、`records`
- Entity：`model/entity`（对应数据库表结构）

---

## 7. 配置类（config）
- `CorsConfig`：全局跨域配置
- `SecurityConfig`：Spring Security + JWT 开关
- `RedisConfig`：RedisTemplate 序列化配置
- `MpConfig`：MyBatis‑Plus 分页拦截器
- `MyMetaObjectHandler`：自动填充 createdAt/updatedAt
- `OpenApiConfig`：OpenAPI/Knife4j 基础配置

---

## 8. Redis 服务封装
- 接口：`service/IRedisService`
- 实现：`service/impl/RedisService`
- 主要能力：KV、Hash、List、Set、计数器
- 示例方法：
  - `setValue(String key, Object value)`：写入 KV
  - `getValue(String key)`：读取 KV
  - `putToMap(String key, String field, Object value)`：写入 Hash
  - `addToList(String key, Object value)`：列表入队
  - `addToSet(String key, Object... values)`：集合添加
  - `increment(String key)`：自增计数

---

## 9. Mapper 与 XML
- Mapper 接口：`mapper/*Mapper.java`
- XML 位置：`src/main/resources/mapper/com/whut/lostandfoundforwhut/mapper/*.xml`

已配置：
```
mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml
```

---

## 10. Swagger / Knife4j
- OpenAPI JSON：`/v3/api-docs`
- Knife4j UI：`/doc.html`

---

## 11. 用户接口（User API）

### 11.1 注册
- `POST /api/users`
- 请求体（JSON）：
  - `email`：邮箱（必填）
  - `password`：密码（必填）
  - `confirmPassword`：确认密码（必填，必须与 `password` 一致）
  - `nickname`：昵称（可选）
- 返回：用户信息 + Token

### 11.2 获取用户信息
- `GET /api/users/{userId}`
- Header：`Authorization: Bearer <token>`

### 11.3 修改密码
- `PUT /api/users/{userId}/password`
- Header：`Authorization: Bearer <token>`
- 请求体（JSON）：
  - `password`：新密码（必填）
  - `confirmPassword`：确认密码（必填，必须与 `password` 一致）

### 11.4 修改昵称
- `PUT /api/users/{userId}/nickname`
- Header：`Authorization: Bearer <token>`
- 请求体（JSON）：
  - `nickname`：昵称（必填）

### 11.5 注销/停用用户
- `DELETE /api/users/{userId}`
- Header：`Authorization: Bearer <token>`
