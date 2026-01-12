## CampusGuessSystem – AI coding guide

### 技术栈与架构
- **运行时**: Spring Boot 3.x, Java 17, Spring Data JPA + MySQL(Hikari); 所有端点挂载在 `/api` 下 ([application.yml](demo/src/main/resources/application.yml))
- **分层架构**: `Controller → Service接口 → ServiceImpl → Repository → Entity`; DTO按模块放在 `model/dto/{domain}/`
- **依赖注入**: ServiceImpl 使用 `@Transactional` + Lombok `@RequiredArgsConstructor` 构造器注入

### 响应与异常处理
- **统一响应**: 所有返回值包装为 `ApiResponse<T>` ([ApiResponse.java](demo/src/main/java/com/campusguess/demo/model/dto/response/ApiResponse.java))
  - 成功: `ApiResponse.success(message, data)`, 新建资源: `ApiResponse.created(message, data)` + HTTP 201
  - 错误: 抛出 `BusinessException(code, message)`, [GlobalExceptionHandler](demo/src/main/java/com/campusguess/demo/exception/GlobalExceptionHandler.java) 统一转换
- **消息语言**: 成功/错误消息使用**中文**

### 安全模型
- [SecurityConfig](demo/src/main/java/com/campusguess/demo/config/SecurityConfig.java) 允许所有请求、禁用CSRF（开发阶段）
- JWT 仅用于登录返回 token ([JwtTokenUtil](demo/src/main/java/com/campusguess/demo/config/JwtTokenUtil.java))，**无 Filter 校验**
- 用户身份通过 `{username}` 路径变量传递（如 `/users/{username}/questions`）

### API约定
- **分页**: 接收1-based `page` 参数，使用 `PageRequest.of(page - 1, size, Sort.by(DESC, "createdAt"))`
- **路由命名**: 用户范围资源 `/users/{username}/{resource}`; 全局资源 `/{resource}`
- **校验**: DTO 使用 `@Valid` 注解，字段校验在 DTO 类定义

### 领域模型 (`model/entity/`)
| 实体 | 关键字段 | 规则 |
|-----|---------|------|
| `User` | username(唯一), role, points | `@PrePersist` 设置 `role="user"`, createdAt; 实现 `UserDetails` |
| `Question` | imageKey, correctLon/Lat, author | 仅作者可删除; 图片用 `imageKey` 非完整URL |
| `BattleRoom` | roomCode, playerA/B, status(WAITING/PLAYING/FINISHED) | 对战房间状态机; 存储双方答案和血量 |
| `Record`/`RecordItem` | 单人游戏记录 | 按回答计算积分 |
| `Friendship` | sender/receiver, status | 双向好友关系，Service层做镜像插入 |

### WebSocket 对战系统 (核心模块)
- **配置**: [WebSocketConfig](demo/src/main/java/com/campusguess/demo/config/WebSocketConfig.java) 启用STOMP协议
- **端点**: `/api/ws-battle` (SockJS降级), 连接时URL参数 `?username=xxx` 标识用户
- **消息前缀**: 客户端发送 `/app/...`, 订阅 `/user/queue/...` 或 `/topic/...`
- **控制器**: [BattleWebSocketController](demo/src/main/java/com/campusguess/demo/controller/BattleWebSocketController.java) 处理:
  - `/app/battle/invite` → 发起邀请
  - `/app/battle/respond` → 接受/拒绝
  - `/app/battle/answer` → 提交答案
  - `/app/battle/quit` → 退出对战
- **在线管理**: [OnlineUserService](demo/src/main/java/com/campusguess/demo/service/OnlineUserService.java) 追踪用户在线和对战状态
- **推送给客户端**: 使用 `SimpMessagingTemplate.convertAndSendToUser(username, destination, message)`

### DTO 命名约定 (`model/dto/{domain}/`)
- 请求: `{Action}Request.java` (如 `BattleInviteRequest`)
- 响应: `{Entity}Response.java` 或 `{Entity}ListResponse.java`
- 对战状态消息: `BattleStateMessage` (含 `MessageType` 枚举: INVITE/GAME_START/ROUND_RESULT/GAME_OVER 等)

### 外部服务
- **图床**: picui.cn, 配置在 `image.host.url/token` ([application.yml](demo/src/main/resources/application.yml))
- 扩展图片功能时使用 `ImageClient` 抽象，不要硬编码URL

### 开发命令 (从 `demo/` 目录)
```bash
./mvnw spring-boot:run      # 启动(DevTools热重载)
./mvnw clean package        # 打包
./mvnw test                 # 测试
```
测试端点记得加 `/api` 前缀 (如 `http://localhost:8080/api/questions`)

### 新功能开发清单
1. Entity → `model/entity/`, 加 `@PrePersist` 处理时间戳
2. Repository → 继承 `JpaRepository`, 复杂查询用 `@Query` JPQL
3. Service接口 → `service/`, Impl → `service/impl/`, 加 `@Transactional`
4. DTO → `model/dto/{domain}/`, 请求DTO加校验注解
5. Controller → 返回 `ResponseEntity<ApiResponse<T>>`, 异常交给全局处理
6. WebSocket功能 → 参考 `BattleWebSocketController`, 用 `@MessageMapping`
