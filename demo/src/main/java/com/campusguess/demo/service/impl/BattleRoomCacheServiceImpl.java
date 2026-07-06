package com.campusguess.demo.service.impl;

import com.campusguess.demo.config.RedisKeyConstants;
import com.campusguess.demo.model.entity.BattleRoom;
import com.campusguess.demo.service.BattleRoomCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 房间实时态 Redis 缓存服务实现
 * 
 * 使用 Redis Hash 存储房间状态，支持：
 * - 全量缓存/读取
 * - 单字段增量更新（减少序列化开销）
 * - 分布式锁（防并发）
 * - TTL 自动过期
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BattleRoomCacheServiceImpl implements BattleRoomCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void cacheRoomState(BattleRoom room) {
        String key = RedisKeyConstants.roomStateKey(room.getRoomCode());
        Map<String, String> stateMap = roomToMap(room);
        redisTemplate.opsForHash().putAll(key, stateMap);
        redisTemplate.expire(key, Duration.ofMinutes(RedisKeyConstants.ROOM_TTL_MINUTES));
        log.debug("房间状态已缓存到Redis: roomCode={}", room.getRoomCode());
    }

    @Override
    public Optional<BattleRoom> getRoomState(String roomCode) {
        String key = RedisKeyConstants.roomStateKey(roomCode);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToRoom(entries));
    }

    @Override
    public void updateRoomField(String roomCode, String field, String value) {
        String key = RedisKeyConstants.roomStateKey(roomCode);
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, Duration.ofMinutes(RedisKeyConstants.ROOM_TTL_MINUTES));
    }

    @Override
    public Optional<String> getRoomField(String roomCode, String field) {
        String key = RedisKeyConstants.roomStateKey(roomCode);
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    @Override
    public void cachePlayerAnswer(String roomCode, String username, String answerJson) {
        String key = RedisKeyConstants.roomAnswerKey(roomCode, username);
        redisTemplate.opsForValue().set(key, answerJson, Duration.ofSeconds(RedisKeyConstants.ANSWER_TTL_SECONDS));
    }

    @Override
    public Optional<String> getPlayerAnswer(String roomCode, String username) {
        String key = RedisKeyConstants.roomAnswerKey(roomCode, username);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    @Override
    public void clearPlayerAnswers(String roomCode, String playerA, String playerB) {
        String keyA = RedisKeyConstants.roomAnswerKey(roomCode, playerA);
        String keyB = RedisKeyConstants.roomAnswerKey(roomCode, playerB);
        redisTemplate.delete(keyA);
        redisTemplate.delete(keyB);
        log.debug("清除玩家答案缓存: roomCode={}, playerA={}, playerB={}", roomCode, playerA, playerB);
    }

    @Override
    public boolean tryLockRoom(String roomCode) {
        String key = RedisKeyConstants.roomLockKey(roomCode);
        String lockValue = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                key, lockValue, Duration.ofSeconds(RedisKeyConstants.LOCK_TTL_SECONDS));
        boolean locked = Boolean.TRUE.equals(success);
        if (!locked) {
            log.warn("房间锁获取失败: roomCode={}", roomCode);
        }
        return locked;
    }

    @Override
    public void unlockRoom(String roomCode) {
        redisTemplate.delete(RedisKeyConstants.roomLockKey(roomCode));
    }

    @Override
    public void evictRoom(String roomCode, String playerA, String playerB) {
        redisTemplate.delete(RedisKeyConstants.roomStateKey(roomCode));
        redisTemplate.delete(RedisKeyConstants.roomLockKey(roomCode));
        clearPlayerAnswers(roomCode, playerA, playerB);
        log.info("房间缓存已清理: roomCode={}, playerA={}, playerB={}", roomCode, playerA, playerB);
    }

    @Override
    public void refreshRoomTTL(String roomCode) {
        String key = RedisKeyConstants.roomStateKey(roomCode);
        redisTemplate.expire(key, Duration.ofMinutes(RedisKeyConstants.ROOM_TTL_MINUTES));
    }

    // ==================== 序列化/反序列化 ====================

    private Map<String, String> roomToMap(BattleRoom room) {
        Map<String, String> map = new HashMap<>();
        map.put(RedisKeyConstants.RoomFields.ROOM_CODE, room.getRoomCode());
        map.put(RedisKeyConstants.RoomFields.PLAYER_A, room.getPlayerA());
        map.put(RedisKeyConstants.RoomFields.PLAYER_B, room.getPlayerB());
        map.put(RedisKeyConstants.RoomFields.PLAYER_A_HEALTH, String.valueOf(room.getPlayerAHealth()));
        map.put(RedisKeyConstants.RoomFields.PLAYER_B_HEALTH, String.valueOf(room.getPlayerBHealth()));
        map.put(RedisKeyConstants.RoomFields.CURRENT_QUESTION_ID,
                room.getCurrentQuestionId() != null ? String.valueOf(room.getCurrentQuestionId()) : "0");
        map.put(RedisKeyConstants.RoomFields.CURRENT_ROUND, String.valueOf(room.getCurrentRound()));
        map.put(RedisKeyConstants.RoomFields.PLAYER_A_ANSWERED, String.valueOf(room.getPlayerAAnswered()));
        map.put(RedisKeyConstants.RoomFields.PLAYER_B_ANSWERED, String.valueOf(room.getPlayerBAnswered()));
        map.put(RedisKeyConstants.RoomFields.PLAYER_A_ANSWER,
                room.getPlayerAAnswer() != null ? room.getPlayerAAnswer() : "");
        map.put(RedisKeyConstants.RoomFields.PLAYER_B_ANSWER,
                room.getPlayerBAnswer() != null ? room.getPlayerBAnswer() : "");
        map.put(RedisKeyConstants.RoomFields.ROUND_HISTORY_JSON,
                room.getRoundHistoryJson() != null ? room.getRoundHistoryJson() : "");
        map.put(RedisKeyConstants.RoomFields.STATUS, room.getStatus().name());
        map.put(RedisKeyConstants.RoomFields.WINNER, room.getWinner() != null ? room.getWinner() : "");
        map.put(RedisKeyConstants.RoomFields.GAME_TYPE, room.getGameType() != null ? room.getGameType() : "好友对战");
        return map;
    }

    private BattleRoom mapToRoom(Map<Object, Object> entries) {
        BattleRoom room = new BattleRoom();
        room.setRoomCode(getStr(entries, RedisKeyConstants.RoomFields.ROOM_CODE));
        room.setPlayerA(getStr(entries, RedisKeyConstants.RoomFields.PLAYER_A));
        room.setPlayerB(getStr(entries, RedisKeyConstants.RoomFields.PLAYER_B));
        room.setPlayerAHealth(getInt(entries, RedisKeyConstants.RoomFields.PLAYER_A_HEALTH, 100));
        room.setPlayerBHealth(getInt(entries, RedisKeyConstants.RoomFields.PLAYER_B_HEALTH, 100));
        room.setCurrentQuestionId(getLong(entries, RedisKeyConstants.RoomFields.CURRENT_QUESTION_ID));
        room.setCurrentRound(getInt(entries, RedisKeyConstants.RoomFields.CURRENT_ROUND, 1));
        room.setPlayerAAnswered(getBool(entries, RedisKeyConstants.RoomFields.PLAYER_A_ANSWERED));
        room.setPlayerBAnswered(getBool(entries, RedisKeyConstants.RoomFields.PLAYER_B_ANSWERED));
        room.setPlayerAAnswer(getStr(entries, RedisKeyConstants.RoomFields.PLAYER_A_ANSWER));
        room.setPlayerBAnswer(getStr(entries, RedisKeyConstants.RoomFields.PLAYER_B_ANSWER));
        room.setRoundHistoryJson(getStr(entries, RedisKeyConstants.RoomFields.ROUND_HISTORY_JSON));
        String statusStr = getStr(entries, RedisKeyConstants.RoomFields.STATUS);
        if (statusStr != null) {
            room.setStatus(BattleRoom.BattleStatus.valueOf(statusStr));
        }
        room.setWinner(getStr(entries, RedisKeyConstants.RoomFields.WINNER));
        room.setGameType(getStr(entries, RedisKeyConstants.RoomFields.GAME_TYPE));
        return room;
    }

    private String getStr(Map<Object, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private int getInt(Map<Object, Object> map, String key, int def) {
        try {
            Object v = map.get(key);
            return v != null ? Integer.parseInt(v.toString()) : def;
        } catch (NumberFormatException e) { return def; }
    }

    private Long getLong(Map<Object, Object> map, String key) {
        try {
            Object v = map.get(key);
            if (v == null || v.toString().isEmpty() || "0".equals(v.toString())) return null;
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) { return null; }
    }

    private boolean getBool(Map<Object, Object> map, String key) {
        Object v = map.get(key);
        return v != null && Boolean.parseBoolean(v.toString());
    }
}