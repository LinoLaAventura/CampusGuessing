package com.campusguess.battle.service;

import java.util.Set;

/**
 * 在线用户服务接口
 * 使用 Redis 管理在线用户状态，替代内存 Map 支持多实例集群
 */
public interface OnlineUserService {

    /**
     * 用户上线
     */
    void userOnline(String username, String sessionId);

    /**
     * 用户下线
     */
    void userOffline(String username, String sessionId);

    /**
     * 心跳续期
     */
    void heartbeat(String username, String sessionId);

    /**
     * 判断用户是否在线
     */
    boolean isOnline(String username);

    /**
     * 获取用户的 sessionId
     */
    String getSessionId(String username);

    /**
     * 根据 sessionId 获取用户名
     */
    String getUsernameBySession(String sessionId);

    /**
     * 获取所有在线用户
     */
    Set<String> getOnlineUsers();

    /**
     * 设置用户当前房间
     */
    void setUserRoom(String username, String roomCode);

    /**
     * 获取用户当前房间
     */
    String getUserRoom(String username);

    /**
     * 清除用户房间状态
     */
    void clearUserRoom(String username);
}