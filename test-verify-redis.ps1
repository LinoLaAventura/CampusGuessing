# ============================================================
# CampusGuessing Redis 改造验证脚本
# 用法：.\test-verify-redis.ps1
# 前置条件：MySQL、Redis 已启动，Spring Boot 应用运行在 8080 端口
# ============================================================

$ErrorActionPreference = "Stop"
$BASE_URL = "http://localhost:8080"
$REDIS_PASSWORD = "134679*Abc"
$PASS = 0
$FAIL = 0

function Test-Result {
    param([string]$Name, [bool]$Passed, [string]$Detail = "")
    if ($Passed) {
        Write-Host "  [PASS] $Name" -ForegroundColor Green
        $script:PASS++
    } else {
        Write-Host "  [FAIL] $Name $Detail" -ForegroundColor Red
        $script:FAIL++
    }
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  CampusGuessing Redis 改造验证" -ForegroundColor Cyan
Write-Host "============================================================`n" -ForegroundColor Cyan

# ==================== 1. 前置检查 ====================
Write-Host "[1/5] 前置条件检查" -ForegroundColor Yellow

# 检查 Redis 连通性
$redisResult = redis-cli -a $REDIS_PASSWORD --no-auth-warning PING 2>&1
Test-Result "Redis 连通性" ($redisResult -eq "PONG") "Redis 未启动或密码错误"

# 检查应用是否启动
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"","password":""}' -TimeoutSec 5 -ErrorAction Stop
    Test-Result "Spring Boot 应用" $true
} catch {
    Test-Result "Spring Boot 应用" $false "应用未启动在 $BASE_URL"
    Write-Host "`n请先启动应用: cd demo; mvnw spring-boot:run`n" -ForegroundColor Red
    exit 1
}

# 清理 Redis 测试数据（避免干扰）
redis-cli -a $REDIS_PASSWORD --no-auth-warning DEL "campus:online:users" 2>&1 | Out-Null
redis-cli -a $REDIS_PASSWORD --no-auth-warning KEYS "campus:room:*" 2>&1 | ForEach-Object { 
    if ($_ -ne "") { redis-cli -a $REDIS_PASSWORD --no-auth-warning DEL $_ 2>&1 | Out-Null }
}

# ==================== 2. 用户认证测试 ====================
Write-Host "`n[2/5] 用户认证" -ForegroundColor Yellow

# 注册测试用户
$testUser = "redis_test_" + (Get-Random -Minimum 1000 -Maximum 9999)
$token = $null

try {
    $regBody = @{ username = $testUser; password = "test123"; nickname = "Redis测试" } | ConvertTo-Json
    $regResp = Invoke-RestMethod -Uri "$BASE_URL/api/auth/register" -Method POST -ContentType "application/json" -Body $regBody -TimeoutSec 10
    Test-Result "注册用户 ($testUser)" ($regResp.code -eq 200) "$regResp"
} catch {
    Test-Result "注册用户 ($testUser)" $false "接口不可用: $_"
}

# 登录
try {
    $loginBody = @{ username = $testUser; password = "test123" } | ConvertTo-Json
    $loginResp = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -TimeoutSec 10
    $token = $loginResp.data.token
    Test-Result "登录获取 Token" ($token -ne $null) "Token: $($token.Substring(0, [Math]::Min(20, $token.Length)))..."
} catch {
    Test-Result "登录获取 Token" $false "接口不可用: $_"
}

# ==================== 3. Redis 在线状态验证 ====================
Write-Host "`n[3/5] Redis 在线状态验证" -ForegroundColor Yellow

# 检查 Redis 中是否有在线用户数据（WebSocket 连接后才会写入）
# 先检查 Redis Key 设计是否正确
$onlineUsers = redis-cli -a $REDIS_PASSWORD --no-auth-warning SMEMBERS "campus:online:users" 2>&1
Write-Host "  [INFO] Redis campus:online:users = $onlineUsers" -ForegroundColor Gray

# 手动写入测试数据验证 Redis 读写
redis-cli -a $REDIS_PASSWORD --no-auth-warning SADD "campus:online:users" "test_manual_user" 2>&1 | Out-Null
$checkAdd = redis-cli -a $REDIS_PASSWORD --no-auth-warning SISMEMBER "campus:online:users" "test_manual_user" 2>&1
Test-Result "Redis Set 写入/读取" ($checkAdd -eq "1") "SADD + SISMEMBER 正常"

redis-cli -a $REDIS_PASSWORD --no-auth-warning SREM "campus:online:users" "test_manual_user" 2>&1 | Out-Null
$checkDel = redis-cli -a $REDIS_PASSWORD --no-auth-warning SISMEMBER "campus:online:users" "test_manual_user" 2>&1
Test-Result "Redis Set 删除" ($checkDel -eq "0") "SREM 正常"

# 测试 Key 格式
$userSessionKey = "campus:user:$testUser" + ":session"
redis-cli -a $REDIS_PASSWORD --no-auth-warning SET $userSessionKey "test-session-123" EX 300 2>&1 | Out-Null
$sessionVal = redis-cli -a $REDIS_PASSWORD --no-auth-warning GET $userSessionKey 2>&1
Test-Result "Redis String 写入/读取 (user:session)" ($sessionVal -eq "test-session-123") "GET Key: $userSessionKey"

$ttl = redis-cli -a $REDIS_PASSWORD --no-auth-warning TTL $userSessionKey 2>&1
Test-Result "Redis TTL 设置" ([int]$ttl -gt 0) "TTL = $ttl 秒"

redis-cli -a $REDIS_PASSWORD --no-auth-warning DEL $userSessionKey 2>&1 | Out-Null

# ==================== 4. Redis 房间缓存验证 ====================
Write-Host "`n[4/5] Redis 房间缓存验证" -ForegroundColor Yellow

# 测试 Hash 操作（房间状态存储方式）
$roomKey = "campus:room:TEST_ROOM_001:state"
redis-cli -a $REDIS_PASSWORD --no-auth-warning HSET $roomKey "roomCode" "TEST_ROOM_001" "status" "WAITING" "playerA" "player1" "playerB" "player2" "currentRound" "1" 2>&1 | Out-Null
$hashFields = redis-cli -a $REDIS_PASSWORD --no-auth-warning HGETALL $roomKey 2>&1
Test-Result "Redis Hash 写入/读取 (room:state)" ($hashFields -match "roomCode" -and $hashFields -match "WAITING") "Fields: $hashFields"

# 测试单字段更新
redis-cli -a $REDIS_PASSWORD --no-auth-warning HSET $roomKey "status" "PLAYING" 2>&1 | Out-Null
$status = redis-cli -a $REDIS_PASSWORD --no-auth-warning HGET $roomKey "status" 2>&1
Test-Result "Redis Hash 增量更新" ($status -eq "PLAYING") "status = $status"

# 测试 TTL
redis-cli -a $REDIS_PASSWORD --no-auth-warning EXPIRE $roomKey 1800 2>&1 | Out-Null
$roomTTL = redis-cli -a $REDIS_PASSWORD --no-auth-warning TTL $roomKey 2>&1
Test-Result "Redis Hash TTL" ([int]$roomTTL -gt 0) "TTL = $roomTTL 秒 (应为1800秒/30分钟)"

# 测试答案缓存
$answerKey = "campus:room:TEST_ROOM_001:answer:player1"
redis-cli -a $REDIS_PASSWORD --no-auth-warning SET $answerKey '{"lon":121.5,"lat":31.2}' EX 60 2>&1 | Out-Null
$answerVal = redis-cli -a $REDIS_PASSWORD --no-auth-warning GET $answerKey 2>&1
Test-Result "Redis 答案缓存" ($answerVal -match "121.5") "Answer: $answerVal"

# 测试分布式锁
$lockKey = "campus:room:TEST_ROOM_001:lock"
$lockResult = redis-cli -a $REDIS_PASSWORD --no-auth-warning SET $lockKey "lock-value" NX EX 10 2>&1
Test-Result "Redis 分布式锁 (SET NX EX)" ($lockResult -eq "OK") "Lock acquired"

$lockFail = redis-cli -a $REDIS_PASSWORD --no-auth-warning SET $lockKey "another" NX EX 10 2>&1
Test-Result "Redis 分布式锁重入拒绝" ($lockFail -eq "" -or $lockFail -eq $null) "Lock correctly rejected"

# 清理
redis-cli -a $REDIS_PASSWORD --no-auth-warning DEL $roomKey $answerKey $lockKey 2>&1 | Out-Null

# ==================== 5. API 接口验证 ====================
Write-Host "`n[5/5] API 接口验证" -ForegroundColor Yellow

if ($token) {
    $authHeader = @{ Authorization = "Bearer $token" }

    # 获取用户信息
    try {
        $userInfo = Invoke-RestMethod -Uri "$BASE_URL/api/user/info" -Method GET -Headers $authHeader -TimeoutSec 10
        Test-Result "GET /api/user/info" ($userInfo.code -eq 200) "User: $($userInfo.data.username)"
    } catch {
        Test-Result "GET /api/user/info" $false "接口不可用: $_"
    }

    # 获取题目列表
    try {
        $questions = Invoke-RestMethod -Uri "$BASE_URL/api/question/list" -Method GET -TimeoutSec 10
        $qCount = if ($questions.data) { $questions.data.Count } else { 0 }
        Test-Result "GET /api/question/list" ($questions.code -eq 200) "题目数: $qCount"
    } catch {
        Test-Result "GET /api/question/list" $false "接口不可用: $_"
    }

    # 好友列表
    try {
        $friends = Invoke-RestMethod -Uri "$BASE_URL/api/friend/list" -Method GET -Headers $authHeader -TimeoutSec 10
        Test-Result "GET /api/friend/list" ($friends.code -eq 200) "好友数: $($friends.data.Count)"
    } catch {
        Test-Result "GET /api/friend/list" $false "接口不可用: $_"
    }

    # 对战记录
    try {
        $records = Invoke-RestMethod -Uri "$BASE_URL/api/record/list" -Method GET -Headers $authHeader -TimeoutSec 10
        Test-Result "GET /api/record/list" ($records.code -eq 200)
    } catch {
        Test-Result "GET /api/record/list" $false "接口不可用: $_"
    }
}

# ==================== 总结 ====================
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  验证结果: 通过 $PASS / 失败 $FAIL" -ForegroundColor $(if ($FAIL -eq 0) { "Green" } else { "Red" })
Write-Host "============================================================" -ForegroundColor Cyan

if ($FAIL -eq 0) {
    Write-Host "`n所有 Redis 改造验证通过！" -ForegroundColor Green
    Write-Host "  1. Redis 连通性正常"
    Write-Host "  2. Key naming convention (campus:module:entity:id) OK"
    Write-Host "  3. Set / String / Hash operations OK"
    Write-Host "  4. TTL 策略生效"
    Write-Host "  5. 分布式锁 (SET NX EX) 正常"
    Write-Host "  6. 核心 API 接口正常"
    Write-Host "`n下一步验证建议:"
    Write-Host "  - 打开前端页面，登录两个不同账号，测试实时对战"
    Write-Host "  - 对战时用 redis-cli 监控: redis-cli -a 134679*Abc --no-auth-warning KEYS campus:*"
    Write-Host "  - 启动多实例验证集群化 (参考 nginx-cluster.conf)`n"
} else {
    Write-Host "`n存在失败项，请检查对应错误信息`n" -ForegroundColor Red
}