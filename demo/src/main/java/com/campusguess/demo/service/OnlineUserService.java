package com.campusguess.demo.service;

import java.util.Set;

/**
 * 在线用户管理服务
 */
public interface OnlineUserService {
    
    /**
     * 用户上线（带sessionId）
     */
    void userOnline(String username, String sessionId);
    
    /**
     * 用户上线
     */
    void userOnline(String username);
    
    /**
     * 用户下线
     */
    void userOffline(String username);
    
    /**
     * 检查用户是否在线
     */
    boolean isUserOnline(String username);
    
    /**
     * 获取所有在线用户
     */
    Set<String> getAllOnlineUsers();
    
    /**
     * 用户进入对战
     */
    void enterBattle(String username, String roomCode);
    
    /**
     * 用户退出对战
     */
    void leaveBattle(String username);
    
    /**
     * 检查用户是否在对战中
     */
    boolean isUserInBattle(String username);
    
    /**
     * 获取用户所在房间代码
     */
    String getUserRoomCode(String username);
    
    /**
     * 获取用户的sessionId
     */
    String getUserSessionId(String username);
    
    /**
     * 通过sessionId获取用户名
     */
    String getUsernameBySessionId(String sessionId);
}
