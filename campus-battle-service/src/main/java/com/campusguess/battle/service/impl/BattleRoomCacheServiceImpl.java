package com.campusguess.battle.service.impl;

import com.campusguess.battle.config.RedisKeyConstants;
import com.campusguess.battle.entity.BattleRoom;
import com.campusguess.battle.service.BattleRoomCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 房间缓存服务实现
 * 使用 Redis 管理房间实时状态，解耦高频读写与数据库持久化
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BattleRoomCacheServiceImpl implements BattleRoomCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void cacheRoom(BattleRoom room) {
        try {
            String key = RedisKeyConstants.roomStateKey(room.getRoomCode());
            String json = objectMapper.writeValueAsString(room);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));
            log.debug("房间缓存成功: {}", room.getRoomCode());
        } catch (Exception e) {
            log.error("缓存房间失败: {}", room.getRoomCode(), e);
        }
    }

    @Override
    public Optional<BattleRoom> getRoom(String roomCode) {
        try {
            String key = RedisKeyConstants.roomStateKey(roomCode);
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            BattleRoom room = objectMapper.readValue(json, BattleRoom.class);
            return Optional.of(room);
        } catch (Exception e) {
            log.error("获取房间缓存失败: {}", roomCode, e);
            return Optional.empty();
        }
    }

    @Override
    public void updateRoom(BattleRoom room) {
        cacheRoom(room);
    }

    @Override
    public void removeRoom(String roomCode) {
        try {
            String key = RedisKeyConstants.roomStateKey(roomCode);
            redisTemplate.delete(key);
            // 同时清理答案
            clearAnswers(roomCode);
            log.debug("房间缓存删除: {}", roomCode);
        } catch (Exception e) {
            log.error("删除房间缓存失败: {}", roomCode, e);
        }
    }

    @Override
    public boolean tryLock(String roomCode) {
        String lockKey = RedisKeyConstants.roomLockKey(roomCode);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock(String roomCode) {
        String lockKey = RedisKeyConstants.roomLockKey(roomCode);
        redisTemplate.delete(lockKey);
    }

    @Override
    public void saveAnswer(String roomCode, String username, String answerJson) {
        String key = RedisKeyConstants.roomAnswersKey(roomCode);
        redisTemplate.opsForHash().put(key, username, answerJson);
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    @Override
    public Optional<String> getAnswer(String roomCode, String username) {
        String key = RedisKeyConstants.roomAnswersKey(roomCode);
        Object answer = redisTemplate.opsForHash().get(key, username);
        return answer != null ? Optional.of(answer.toString()) : Optional.empty();
    }

    @Override
    public void clearAnswers(String roomCode) {
        String key = RedisKeyConstants.roomAnswersKey(roomCode);
        redisTemplate.delete(key);
    }
}