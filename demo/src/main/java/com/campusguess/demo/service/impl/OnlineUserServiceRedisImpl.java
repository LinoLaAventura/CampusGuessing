package com.campusguess.demo.service.impl;

import com.campusguess.demo.config.RedisKeyConstants;
import com.campusguess.demo.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 在线用户管理服务 - Redis 实现
 * 
 * 替代原来的 ConcurrentHashMap 内存实现，支持多实例共享在线状态。
 * 
 * 启用条件：online-user.store=redis（默认启用）
 * 回退到内存实现：online-user.store=memory
 * 
 * 数据一致性保证：
 *   - onlineUsers 集合 + user→session + session→user 三者同步更新
 *   - userBattleStatus 使用独立的 String key，原子操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "online-user.store", havingValue = "redis", matchIfMissing = true)
public class OnlineUserServiceRedisImpl implements OnlineUserService {

    private final StringRedisTemplate stringRedisTemplate;

    // ==================== 在线状态管理 ====================

    @Override
    public void userOnline(String username, String sessionId) {
        // 如果用户已在线（重复连接），清理旧session
        String oldSessionId = stringRedisTemplate.opsForValue()
                .get(RedisKeyConstants.userSessionKey(username));
        if (oldSessionId != null) {
            log.warn("用户 {} 重复连接，旧session={}, 新session={}", username, oldSessionId, sessionId);
            // 移除旧session映射
            stringRedisTemplate.delete(RedisKeyConstants.sessionUserKey(oldSessionId));
        }

        // 添加在线用户集合
        stringRedisTemplate.opsForSet().add(RedisKeyConstants.ONLINE_USERS, username);

        // 用户 → session（带TTL，心跳续期）
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.userSessionKey(username),
                sessionId,
                RedisKeyConstants.ONLINE_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        // session → 用户（带TTL）
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.sessionUserKey(sessionId),
                username,
                RedisKeyConstants.ONLINE_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        log.info("用户上线(Redis): {}, sessionId={}", username, sessionId);
    }

    @Override
    public void userOnline(String username) {
        stringRedisTemplate.opsForSet().add(RedisKeyConstants.ONLINE_USERS, username);
        log.info("用户上线(Redis): {}", username);
    }

    @Override
    public void userOffline(String username) {
        // 清理 session 映射
        String sessionId = stringRedisTemplate.opsForValue()
                .get(RedisKeyConstants.userSessionKey(username));
        if (sessionId != null) {
            stringRedisTemplate.delete(RedisKeyConstants.sessionUserKey(sessionId));
        }

        // 清理所有在线状态
        stringRedisTemplate.delete(RedisKeyConstants.userSessionKey(username));
        stringRedisTemplate.opsForSet().remove(RedisKeyConstants.ONLINE_USERS, username);
        stringRedisTemplate.delete(RedisKeyConstants.userRoomKey(username));

        log.info("用户下线(Redis): {}", username);
    }

    @Override
    public boolean isUserOnline(String username) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(RedisKeyConstants.ONLINE_USERS, username)
        );
    }

    @Override
    public Set<String> getAllOnlineUsers() {
        return stringRedisTemplate.opsForSet().members(RedisKeyConstants.ONLINE_USERS);
    }

    // ==================== 对战状态管理 ====================

    @Override
    public void enterBattle(String username, String roomCode) {
        // 用户 → roomCode 映射（跟随对战生命周期，不设TTL；leaveBattle 时删除）
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.userRoomKey(username),
                roomCode
        );
        log.info("用户进入对战(Redis): {}, 房间: {}", username, roomCode);
    }

    @Override
    public void leaveBattle(String username) {
        stringRedisTemplate.delete(RedisKeyConstants.userRoomKey(username));
        log.info("用户退出对战(Redis): {}", username);
    }

    @Override
    public boolean isUserInBattle(String username) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(RedisKeyConstants.userRoomKey(username))
        );
    }

    @Override
    public String getUserRoomCode(String username) {
        return stringRedisTemplate.opsForValue()
                .get(RedisKeyConstants.userRoomKey(username));
    }

    // ==================== Session 管理 ====================

    @Override
    public String getUserSessionId(String username) {
        return stringRedisTemplate.opsForValue()
                .get(RedisKeyConstants.userSessionKey(username));
    }

    @Override
    public String getUsernameBySessionId(String sessionId) {
        return stringRedisTemplate.opsForValue()
                .get(RedisKeyConstants.sessionUserKey(sessionId));
    }
}