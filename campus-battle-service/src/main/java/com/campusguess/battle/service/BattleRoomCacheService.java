package com.campusguess.battle.service;

import com.campusguess.battle.entity.BattleRoom;

import java.util.Optional;

/**
 * 房间缓存服务接口
 * 使用 Redis 管理房间实时状态，解耦高频读写与数据库持久化
 */
public interface BattleRoomCacheService {

    /**
     * 缓存房间状态到 Redis
     */
    void cacheRoom(BattleRoom room);

    /**
     * 从 Redis 获取房间状态
     */
    Optional<BattleRoom> getRoom(String roomCode);

    /**
     * 更新房间状态到 Redis
     */
    void updateRoom(BattleRoom room);

    /**
     * 删除房间缓存
     */
    void removeRoom(String roomCode);

    /**
     * 获取分布式锁
     */
    boolean tryLock(String roomCode);

    /**
     * 释放分布式锁
     */
    void unlock(String roomCode);

    /**
     * 保存玩家答案到 Redis
     */
    void saveAnswer(String roomCode, String username, String answerJson);

    /**
     * 获取玩家答案
     */
    Optional<String> getAnswer(String roomCode, String username);

    /**
     * 清除房间答案
     */
    void clearAnswers(String roomCode);
}