package com.campusguess.demo.service;

import com.campusguess.demo.model.entity.BattleRoom;

import java.util.Map;
import java.util.Optional;

/**
 * 房间实时态 Redis 缓存服务
 * 
 * 设计原则：
 * - Redis 缓存"热"数据（高频读写的实时状态），数据库保持"冷"数据（持久化记录）
 * - 所有写操作：先写 Redis，再异步/同步写 DB
 * - 所有读操作：优先读 Redis，miss 时回退到 DB
 * - 房间结束时：主动清理 Redis 缓存，DB 保留完整记录
 * 
 * 缓存的生命周期：
 * - 创建房间 → 写入 Redis 缓存
 * - 游戏进行中 → 更新 Redis 缓存
 * - 游戏结束 → 删除 Redis 缓存，DB 保留
 * - 超时未活动 → TTL 自动过期（30分钟）
 */
public interface BattleRoomCacheService {

    /**
     * 缓存房间状态快照到 Redis
     * 在创建房间、接受邀请、提交答案、回合结算后调用
     */
    void cacheRoomState(BattleRoom room);

    /**
     * 从 Redis 获取房间状态快照
     * 如果 Redis 中没有，返回 Optional.empty()（调用方应回退到 DB）
     */
    Optional<BattleRoom> getRoomState(String roomCode);

    /**
     * 更新房间部分字段（Hash 增量更新）
     * 用于高频更新的字段（如作答状态、血量），避免全量序列化开销
     */
    void updateRoomField(String roomCode, String field, String value);

    /**
     * 获取房间单个字段值
     */
    Optional<String> getRoomField(String roomCode, String field);

    /**
     * 缓存玩家答案（回合级临时数据，TTL 60秒）
     */
    void cachePlayerAnswer(String roomCode, String username, String answerJson);

    /**
     * 获取玩家答案
     */
    Optional<String> getPlayerAnswer(String roomCode, String username);

    /**
     * 清除指定房间的玩家答案缓存（新回合开始时调用）
     * @param roomCode 房间代码
     * @param playerA 玩家A的用户名
     * @param playerB 玩家B的用户名
     */
    void clearPlayerAnswers(String roomCode, String playerA, String playerB);

    /**
     * 获取房间分布式锁
     * 用于防止并发操作（如同步提交答案、重复结算）
     * 
     * @return true=获取锁成功，false=锁已被占用
     */
    boolean tryLockRoom(String roomCode);

    /**
     * 释放房间分布式锁
     */
    void unlockRoom(String roomCode);

    /**
     * 删除房间缓存（游戏结束时调用）
     * @param roomCode 房间代码
     * @param playerA 玩家A的用户名
     * @param playerB 玩家B的用户名
     */
    void evictRoom(String roomCode, String playerA, String playerB);

    /**
     * 刷新房间缓存 TTL（心跳续期）
     */
    void refreshRoomTTL(String roomCode);
}