@echo off
chcp 65001 >nul
echo ============================================================
echo   CampusGuessing Redis 数据结构验证
echo ============================================================

set PASS=0
set FAIL=0

echo.
echo [1] 分布式锁 (SET NX EX)
for /f "tokens=*" %%i in ('redis-cli SET "campus:room:TEST:lock" "test" NX EX 10') do set LOCK1=%%i
for /f "tokens=*" %%i in ('redis-cli SET "campus:room:TEST:lock" "test2" NX EX 10') do set LOCK2=%%i
if "%LOCK1%"=="OK" if "%LOCK2%"=="" (echo   [PASS] 首次OK, 重入返回nil) else (echo   [FAIL] LOCK1=%LOCK1% LOCK2=%LOCK2%)
redis-cli DEL "campus:room:TEST:lock" >nul

echo.
echo [2] 房间状态 Hash
redis-cli HSET "campus:room:TEST:state" "roomCode" "TEST" "status" "PLAYING" "currentRound" "2" "playerA" "user1" "playerB" "user2" >nul
for /f "tokens=*" %%i in ('redis-cli HGET "campus:room:TEST:state" "status"') do set HSTATUS=%%i
if "%HSTATUS%"=="PLAYING" (echo   [PASS] HGET status = %HSTATUS%) else (echo   [FAIL] HGET status = %HSTATUS%)

echo.
echo [3] 增量更新 boolean 字段
redis-cli HSET "campus:room:TEST:state" "playerAAnswered" "true" "playerBAnswered" "false" >nul
for /f "tokens=*" %%i in ('redis-cli HGET "campus:room:TEST:state" "playerAAnswered"') do set A=%%i
for /f "tokens=*" %%i in ('redis-cli HGET "campus:room:TEST:state" "playerBAnswered"') do set B=%%i
if "%A%"=="true" if "%B%"=="false" (echo   [PASS] A=%A% B=%B%) else (echo   [FAIL] A=%A% B=%B%)

echo.
echo [4] 答案缓存 (TTL 60s)
redis-cli SET "campus:room:TEST:answer:user1" "{\"longitude\":\"121.5\",\"latitude\":\"31.2\"}" EX 60 >nul
redis-cli SET "campus:room:TEST:answer:user2" "{\"longitude\":\"121.4\",\"latitude\":\"31.3\"}" EX 60 >nul
for /f "tokens=*" %%i in ('redis-cli GET "campus:room:TEST:answer:user1"') do set A1=%%i
for /f "tokens=*" %%i in ('redis-cli GET "campus:room:TEST:answer:user2"') do set A2=%%i
for /f "tokens=*" %%i in ('redis-cli TTL "campus:room:TEST:answer:user1"') do set TTL1=%%i
echo   user1: %A1%
echo   user2: %A2%
if %TTL1% GTR 0 (echo   [PASS] TTL = %TTL1%s) else (echo   [FAIL] TTL = %TTL1%s)

echo.
echo [5] 清除答案 (DEL)
redis-cli DEL "campus:room:TEST:answer:user1" "campus:room:TEST:answer:user2" >nul
for /f "tokens=*" %%i in ('redis-cli GET "campus:room:TEST:answer:user1"') do set C1=%%i
for /f "tokens=*" %%i in ('redis-cli GET "campus:room:TEST:answer:user2"') do set C2=%%i
if "%C1%"=="" if "%C2%"=="" (echo   [PASS] 两个答案均已删除) else (echo   [FAIL] C1=%C1% C2=%C2%)

echo.
echo [6] evictRoom 全量清理
redis-cli HSET "campus:room:TEST:state" "roomCode" "TEST" "status" "PLAYING" >nul
redis-cli SET "campus:room:TEST:answer:user1" "{}" EX 60 >nul
redis-cli SET "campus:room:TEST:answer:user2" "{}" EX 60 >nul
redis-cli SET "campus:room:TEST:lock" "x" EX 10 >nul
redis-cli DEL "campus:room:TEST:state" "campus:room:TEST:answer:user1" "campus:room:TEST:answer:user2" "campus:room:TEST:lock" >nul
for /f "tokens=*" %%i in ('redis-cli KEYS "campus:room:TEST:*"') do set RESIDUAL=%%i
if "%RESIDUAL%"=="" (echo   [PASS] 无残留 key) else (echo   [FAIL] 残留: %RESIDUAL%)

echo.
echo ============================================================
echo   验证完成
echo ============================================================