package com.campusguess.demo.config;

/**
 * Redis Key 设计规范
 * 
 * 命名规范：campus:{模块}:{实体}:{标识}
 * 
 * 分类：
 *   1. 在线状态层 —— 替代 OnlineUserServiceImpl 的内存 Map
 *   2. 房间实时态层 —— BattleRoom 高频读写字段的缓存
 *   3. 作答状态层 —— 回合级临时数据
 * 
 * Key 设计原则：
 *   - 所有 Key 统一前缀 campus: 避免与其他系统冲突
 *   - 使用 : 分隔层级，便于 Redis 可视化工具分组查看
 *   - TTL 策略：心跳类短 TTL，房间类跟随游戏生命周期
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    // ==================== 1. 在线状态层 ====================

    /** 在线用户集合 (Set) */
    public static final String ONLINE_USERS = "campus:online:users";

    /** 用户 → sessionId 映射 (String)，TTL: 30分钟（心跳续期） */
    public static final String USER_SESSION = "campus:user:%s:session";

    /** sessionId → 用户映射 (String)，TTL: 30分钟 */
    public static final String SESSION_USER = "campus:session:%s:user";

    /** 用户 → roomCode 映射 (String)，TTL: 跟随对战生命周期 */
    public static final String USER_ROOM = "campus:user:%s:room";

    // ==================== 2. 房间实时态层 ====================

    /** 房间状态快照 (Hash)，TTL: 30分钟（对战结束后主动删除） */
    public static final String ROOM_STATE = "campus:room:%s:state";

    /** 房间分布式锁 (String)，TTL: 10秒（防止并发操作） */
    public static final String ROOM_LOCK = "campus:room:%s:lock";

    // ==================== 3. 作答状态层 ====================

    /** 玩家当前回合答案 (String, JSON)，TTL: 60秒 */
    public static final String ROOM_ANSWER = "campus:room:%s:answer:%s";

    // ==================== Hash Field 定义 ====================

    /** 房间状态 Hash 中的字段名 */
    public static final class RoomFields {
        private RoomFields() {}
        
        public static final String ROOM_CODE = "roomCode";
        public static final String PLAYER_A = "playerA";
        public static final String PLAYER_B = "playerB";
        public static final String PLAYER_A_HEALTH = "playerAHealth";
        public static final String PLAYER_B_HEALTH = "playerBHealth";
        public static final String CURRENT_QUESTION_ID = "currentQuestionId";
        public static final String CURRENT_ROUND = "currentRound";
        public static final String PLAYER_A_ANSWERED = "playerAAnswered";
        public static final String PLAYER_B_ANSWERED = "playerBAnswered";
        public static final String STATUS = "status";
        public static final String WINNER = "winner";
        public static final String GAME_TYPE = "gameType";
    }

    // ==================== TTL 常量 ====================

    /** 在线状态 TTL：30分钟 */
    public static final long ONLINE_TTL_MINUTES = 30;

    /** 房间状态 TTL：30分钟 */
    public static final long ROOM_TTL_MINUTES = 30;

    /** 作答状态 TTL：60秒 */
    public static final long ANSWER_TTL_SECONDS = 60;

    /** 分布式锁 TTL：10秒 */
    public static final long LOCK_TTL_SECONDS = 10;

    // ==================== 工具方法 ====================

    public static String userSessionKey(String username) {
        return String.format(USER_SESSION, username);
    }

    public static String sessionUserKey(String sessionId) {
        return String.format(SESSION_USER, sessionId);
    }

    public static String userRoomKey(String username) {
        return String.format(USER_ROOM, username);
    }

    public static String roomStateKey(String roomCode) {
        return String.format(ROOM_STATE, roomCode);
    }

    public static String roomLockKey(String roomCode) {
        return String.format(ROOM_LOCK, roomCode);
    }

    public static String roomAnswerKey(String roomCode, String username) {
        return String.format(ROOM_ANSWER, roomCode, username);
    }
}