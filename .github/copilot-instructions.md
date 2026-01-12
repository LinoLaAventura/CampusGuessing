## CampusGuessSystem – AI coding guide

### 技术栈与架构
- **后端运行时**: Spring Boot 4.0.1, Java 17, Spring Data JPA + MyBatis-Plus + MySQL(Hikari连接池)
- **前端**: React (Dashboard组件位于 `frontend/src/pages/`)
- **Web层**: Spring WebMVC + WebSocket (STOMP协议); 所有端点挂载在 `/api` 下 ([application.yml](demo/src/main/resources/application.yml#L3))
- **分层架构**: `Controller → Service接口 → ServiceImpl → Repository → Entity`
  - DTO按领域模块分包: `model/dto/{domain}/` (auth, battle, comment, question, record, user, response)
  - 实体类: `model/entity/` 统一使用 `@PrePersist` 自动设置创建时间
- **依赖注入**: ServiceImpl 使用 Lombok `@RequiredArgsConstructor` 构造器注入 + `@Transactional` 事务管理
- **配置管理**: 敏感配置(如JWT密钥、图床token)统一在 [application.yml](demo/src/main/resources/application.yml) 管理
  - ⚠️ **敏感信息**: `application.yml` 含数据库密码、JWT密钥、图床token (生产环境需迁移至环境变量)

### 响应与异常处理
- **统一响应包装**: 所有Controller返回 `ResponseEntity<ApiResponse<T>>` ([ApiResponse.java](demo/src/main/java/com/campusguess/demo/model/dto/response/ApiResponse.java))
  - 成功: `ApiResponse.success(message, data)` → HTTP 200
  - 新建资源: `ApiResponse.created(message, data)` → HTTP 201
  - 示例: `return ResponseEntity.ok(ApiResponse.success("查询成功", data));`
- **异常处理**: Service层抛出 `BusinessException(code, message)`, [GlobalExceptionHandler](demo/src/main/java/com/campusguess/demo/exception/GlobalExceptionHandler.java) 全局拦截转换:
  - `@ExceptionHandler(BusinessException.class)` → 返回业务错误码和消息
  - `@ExceptionHandler(MethodArgumentNotValidException.class)` → 400参数校验失败
  - `@ExceptionHandler(SQLIntegrityConstraintViolationException.class)` → 409唯一索引冲突(如用户名重复)
- **消息语言**: 所有成功/错误消息使用**中文** (如 "题目创建成功", "用户名已存在")

### 安全模型 (开发环境配置)
- [SecurityConfig](demo/src/main/java/com/campusguess/demo/config/SecurityConfig.java): 允许所有请求 (`permitAll()`), 禁用CSRF
- **JWT机制**: 登录时生成token ([JwtTokenUtil](demo/src/main/java/com/campusguess/demo/config/JwtTokenUtil.java)), **无Filter校验** (仅做标识用)
- **身份传递**: 用户身份通过路径变量 `{username}` 传递 (如 `POST /users/{username}/questions`)
- ⚠️ 生产环境需启用Filter校验和CSRF保护

### API设计约定
- **分页查询**: 接收1-based `page` 参数, 内部转换: `PageRequest.of(page - 1, size, Sort.by(DESC, "createdAt"))`
  - 示例: `GET /questions?page=1&size=20` → 获取第1页(实际offset=0), 按创建时间倒序
- **路由命名规则**:
  - 用户范围资源: `/users/{username}/{resource}` (如 `/users/alice/questions`)
  - 全局资源: `/{resource}` (如 `/questions`)
- **参数校验**: DTO类使用 `@Valid` + Bean Validation注解 (如 `@NotBlank`, `@Size`)

### 领域模型详解 (`model/entity/`)
| 实体 | 关键字段 | 特殊规则 |
|-----|---------|---------|
| `User` | username(唯一), role, points | 实现 `UserDetails`; `@PrePersist` 设置 `role="user"` |
| `Question` | imageKey, correctLon/Lat, author | 图片用 `imageKey` (非完整URL); 仅作者可删除 |
| `BattleRoom` | roomCode(唯一), playerA/B, status, health | 状态机: `WAITING→PLAYING→FINISHED`; 存储双方答案(JSON)和血量 |
| `Comment` | content, user(ManyToOne), question(ManyToOne) | 懒加载关联; `likeCount` 默认0 |
| `CommentLike` | user, comment | 点赞关联表 (避免重复点赞) |
| `Record`/`RecordItem` | 1对多关系 | 单人游戏记录; `RecordItem` 存储每道题得分 |
| `Friendship` | sender, receiver, status | Service层创建时做**镜像插入** (双向好友关系) |

### WebSocket实时对战系统 (核心功能)
- **配置类**: [WebSocketConfig](demo/src/main/java/com/campusguess/demo/config/WebSocketConfig.java)
  - 启用STOMP协议: `@EnableWebSocketMessageBroker`
  - 端点: `/api/ws-battle` (支持SockJS降级)
  - 连接URL参数: `?username=xxx` (通过 `HandshakeInterceptor` 提取到session attributes)
- **消息路由规则**:
  - 客户端发送: `/app/battle/{action}` → 路由到 `@MessageMapping` 方法
  - 客户端订阅: `/user/queue/battle/state` (单播) 或 `/topic/...` (广播)
- **控制器**: [BattleWebSocketController](demo/src/main/java/com/campusguess/demo/controller/BattleWebSocketController.java) 处理:
  - `/app/battle/invite` → 发起对战邀请 (检查对方在线状态)
  - `/app/battle/respond` → 接受/拒绝邀请 (状态: `WAITING→PLAYING`)
  - `/app/battle/answer` → 提交答案 (计算距离、扣血; 双方都答完后结算回合)
  - `/app/battle/quit` → 退出对战 (清理房间、更新在线状态)
- **在线状态管理**: [OnlineUserService](demo/src/main/java/com/campusguess/demo/service/OnlineUserService.java)
  - 维护 `Map<username, sessionId>` 和 `Map<username, roomCode>`
  - 监听 `SessionConnectedEvent`/`SessionDisconnectEvent` 自动更新
- **消息推送**: 使用 `SimpMessagingTemplate.convertAndSendToUser(username, "/queue/battle/state", message)`
- **状态消息**: [BattleStateMessage](demo/src/main/java/com/campusguess/demo/model/dto/battle/BattleStateMessage.java) 包含:
  - `MessageType` 枚举: `INVITE`, `INVITE_ACCEPTED`, `GAME_START`, `ROUND_RESULT`, `GAME_OVER`, `PLAYER_QUIT`
  - `RoundResult` 嵌套对象: 双方距离、扣血量、剩余血量

### DTO命名约定 (`model/dto/{domain}/`)
- **请求DTO**: `{Action}Request.java`
  - 示例: `BattleInviteRequest` (fromUsername, toUsername), `CreateQuestionRequest`
  - 包含 `@NotBlank`, `@Size` 等校验注解
- **响应DTO**: `{Entity}Response.java` 或 `{Entity}ListResponse.java`
  - 示例: `QuestionResponse`, `QuestionListResponse` (含分页元数据: totalPages, totalElements)
- **消息DTO**: `{Domain}Message.java` (用于WebSocket)
  - 示例: `BattleStateMessage` (含状态类型、消息文本、房间信息)

### 外部服务集成
- **图床API**: picui.cn, 配置在 [application.yml](demo/src/main/resources/application.yml#L36-L38)
  - `image.host.url` + `image.host.token` (Bearer token)
  - 使用 `ImageClient` Service抽象 (避免硬编码URL)
  - Question实体存储 `imageKey`, 前端拼接完整URL

### 开发工作流 (在 `demo/` 目录下执行)
```bash
# 启动开发服务器 (DevTools热重载, 监听8080端口)
./mvnw spring-boot:run

# Windows环境使用 (推荐)
mvnw.cmd spring-boot:run

# 构建JAR包 (输出到 target/demo-0.0.1-SNAPSHOT.jar)
./mvnw clean package

# 运行测试
./mvnw test

# 测试端点需加 /api 前缀
# 示例: http://localhost:8080/api/questions
#       ws://localhost:8080/api/ws-battle?username=alice
```

**常见问题排查**:
- **数据库连接失败**: 检查 `application.yml` 中 `spring.datasource.url`/`username`/`password` 是否正确
- **端口占用**: 修改 `application.yml` 中 `server.port` 或停止占用8080端口的进程
- **WebSocket握手失败**: 确认URL包含 `?username=xxx` 参数, 参考 [WebSocket接口对接文档.md](WebSocket接口对接文档.md)
- **依赖下载慢**: 启用 `pom.xml` 中注释的阿里云镜像配置 (约第33-43行)

### 新功能开发清单
1. **Entity** → `model/entity/`
   - 添加 `@PrePersist` 方法处理 `createdAt` 时间戳
   - 关联关系用 `@ManyToOne(fetch = LAZY)` (避免N+1查询)
2. **Repository** → 继承 `JpaRepository<Entity, ID>`
   - 复杂查询用 `@Query` 注解编写JPQL (如 `findByUsername`)
3. **Service接口** → `service/` 目录, Impl → `service/impl/`
   - Impl类添加 `@Service`, `@Transactional`, `@RequiredArgsConstructor`
   - 业务异常抛出 `BusinessException(code, message)`
4. **DTO** → `model/dto/{domain}/`
   - 请求DTO添加校验注解 (如 `@NotBlank(message = "用户名不能为空")`)
   - 响应DTO包含必要字段 (避免暴露敏感信息如密码)
5. **Controller** → 统一返回 `ResponseEntity<ApiResponse<T>>`
   - 成功: `ResponseEntity.ok(ApiResponse.success(...))`
   - 异常: 抛给 `GlobalExceptionHandler` 处理
6. **WebSocket功能** → 参考 `BattleWebSocketController`
   - 使用 `@MessageMapping` 处理客户端消息
   - 使用 `SimpMessagingTemplate` 推送消息到指定用户/主题

### 调试提示
- **WebSocket连接**: 查看浏览器控制台 `ws://localhost:8080/api/ws-battle` 握手日志
- **数据库查询**: 启用 `spring.jpa.show-sql=true` 查看生成的SQL (默认已启用)
- **热重载**: DevTools监听类路径变化, 保存文件后自动重启 (配置不变时约2-3秒)
- **测试HTML**: [static/battle-demo.html](demo/src/main/resources/static/battle-demo.html) 提供WebSocket对战测试页面
  - 访问: `http://localhost:8080/api/battle-demo.html`
  - 打开多个浏览器标签模拟多用户对战

### 重要文档参考
- [WebSocket接口对接文档.md](WebSocket接口对接文档.md): WebSocket连接/订阅/消息格式详解 (含前端示例代码)
- [对战系统更新说明.md](对战系统更新说明.md): 对战记录存储机制 (BattleRoundRecord → Record/RecordItem 转换逻辑)
