@echo off
chcp 65001 >nul
title CampusGuessing 集群启动脚本
echo ============================================================
echo   CampusGuessing - 集群模式启动脚本
echo ============================================================
echo.

REM ==================== 环境检查 ====================
echo [1/5] 检查环境...
echo.

REM 检查 Redis
echo   检查 Redis...
redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    echo   [错误] Redis 未启动！请先启动 redis-server
    pause
    exit /b 1
)
echo   [OK] Redis 已连接

REM 检查 RabbitMQ
echo   检查 RabbitMQ...
docker ps --filter name=rabbitmq --format "{{.Status}}" 2>nul | findstr "Up" >nul
if %errorlevel% neq 0 (
    echo   [错误] RabbitMQ 未启动！请先执行：docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 -p 61613:61613 rabbitmq:3-management
    pause
    exit /b 1
)
echo   [OK] RabbitMQ 已运行

echo.
echo [2/5] 检查构建产物...
if not exist "target\demo-0.0.1-SNAPSHOT.jar" (
    echo   [信息] 未找到 JAR，正在构建...
    call mvnw.cmd clean package -DskipTests -q
    if %errorlevel% neq 0 (
        echo   [错误] 构建失败！
        pause
        exit /b 1
    )
    echo   [OK] 构建完成
) else (
    echo   [OK] JAR 已存在
)

echo.
echo [3/5] 启动实例 1 (端口 8080)...
start "CampusGuessing-8080" cmd /c "java -jar target\demo-0.0.1-SNAPSHOT.jar --server.port=8080 --broker.type=external --online-user.store=redis"
echo   实例 1 已启动

REM 等待实例 1 启动
timeout /t 8 /nobreak >nul

echo.
echo [4/5] 启动实例 2 (端口 8081)...
start "CampusGuessing-8081" cmd /c "java -jar target\demo-0.0.1-SNAPSHOT.jar --server.port=8081 --broker.type=external --online-user.store=redis"
echo   实例 2 已启动

echo.
echo [5/5] 启动 Nginx...
start "Nginx" D:\nginx\nginx-1.30.3\nginx.exe -c D:\nginx\nginx-1.30.3\conf\nginx-cluster.conf
echo   Nginx 已启动

echo.
echo ============================================================
echo   集群启动完成！
echo.
echo   实例列表：
echo     - 实例 1: http://localhost:8080/api
echo     - 实例 2: http://localhost:8081/api
echo     - Nginx:   http://localhost/api  (负载均衡)
echo     - WS:      ws://localhost/api/ws-battle
echo     - RabbitMQ: http://localhost:15672 (guest/guest)
echo.
echo   按任意键停止所有实例...
echo ============================================================
pause >nul

REM 停止所有
echo 正在停止所有实例...
taskkill /fi "WINDOWTITLE eq CampusGuessing-8080" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq CampusGuessing-8081" /f >nul 2>&1
D:\nginx\nginx-1.30.3\nginx.exe -c D:\nginx\nginx-1.30.3\conf\nginx-cluster.conf -s quit >nul 2>&1
echo 所有实例已停止
pause