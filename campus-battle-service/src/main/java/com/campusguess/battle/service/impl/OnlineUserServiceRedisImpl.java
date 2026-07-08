package com.campusguess.battle.service.impl;

import com.campusguess.battle.config.RedisKeyConstants;
import com.campusguess.battle.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * 在线用户服务 Redis 实现
 * 使用 Redis 管理在线用户状态，支持多实例集群
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineUserServiceRedisImpl implements OnlineUserService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void userOnline(String username, String sessionId) {
        // 用户上线，记录 session 映射
        String sessionKey = RedisKeyConstants.userSessionKey(username);
        redisTemplate.opsForValue().set(sessionKey, sessionId, Duration.ofHours(2));

        String userKey = RedisKeyConstants.sessionUserKey(sessionId);
        redisTemplate.opsForValue().set(userKey, username, Duration.ofHours(2));

        redisTemplate.opsForSet().add(RedisKeyConstants.ONLINE_USERS, username);
        log.info("用户上线: {}, sessionId={}", username, sessionId);
    }

    @Override
    public void userOffline(String username, String sessionId) {
        String sessionKey = RedisKeyConstants.userSessionKey(username);
        redisTemplate.delete(sessionKey);

        String userKey = RedisKeyConstants.sessionUserKey(sessionId);
        redisTemplate.delete(userKey);

        redisTemplate.opsForSet().remove(RedisKeyConstants.ONLINE_USERS, username);
        clearUserRoom(username);
        log.info("用户下线: {}", username);
    }

    @Override
    public void heartbeat(String username, String sessionId) {
        // 续期 session
        String sessionKey = RedisKeyConstants.userSessionKey(username);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
            redisTemplate.expire(sessionKey, Duration.ofHours(2));
        }

        String userKey = RedisKeyConstants.sessionUserKey(sessionId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
            redisTemplate.expire(userKey, Duration.ofHours(2));
        }

        // 续期房间状态
        String roomKey = RedisKeyConstants.userRoomKey(username);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(roomKey))) {
            redisTemplate.expire(roomKey, Duration.ofHours(2));
        }
    }

    @Override
    public boolean isOnline(String username) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(RedisKeyConstants.ONLINE_USERS, username));
    }

    @Override
    public String getSessionId(String username) {
        return redisTemplate.opsForValue().get(RedisKeyConstants.userSessionKey(username));
    }

    @Override
    public String getUsernameBySession(String sessionId) {
        return redisTemplate.opsForValue().get(RedisKeyConstants.sessionUserKey(sessionId));
    }

    @Override
    public Set<String> getOnlineUsers() {
        return redisTemplate.opsForSet().members(RedisKeyConstants.ONLINE_USERS);
    }

    @Override
    public void setUserRoom(String username, String roomCode) {
        redisTemplate.opsForValue().set(
                RedisKeyConstants.userRoomKey(username),
                roomCode,
                Duration.ofHours(2));
        log.info("用户 {} 进入房间: {}", username, roomCode);
    }

    @Override
    public String getUserRoom(String username) {
        return redisTemplate.opsForValue().get(RedisKeyConstants.userRoomKey(username));
    }

    @Override
    public void clearUserRoom(String username) {
        redisTemplate.delete(RedisKeyConstants.userRoomKey(username));
        log.info("用户 {} 离开房间", username);
    }
}