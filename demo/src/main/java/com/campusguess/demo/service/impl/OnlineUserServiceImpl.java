package com.campusguess.demo.service.impl;

import com.campusguess.demo.service.OnlineUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户管理服务实现
 */
@Service
@Slf4j
public class OnlineUserServiceImpl implements OnlineUserService {
    
    // 在线用户集合
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    
    // 用户对战状态 username -> roomCode
    private final Map<String, String> userBattleStatus = new ConcurrentHashMap<>();
    
    // 用户与sessionId的映射 username -> sessionId
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    
    // sessionId与用户的映射 sessionId -> username
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    @Override
    public void userOnline(String username, String sessionId) {
        // 如果用户已在线（重复连接），记录旧session将被替换
        if (onlineUsers.contains(username)) {
            String oldSessionId = userSessionMap.get(username);
            log.warn("用户 {} 重复连接，旧session={}, 新session={}", username, oldSessionId, sessionId);
            // 移除旧session映射
            if (oldSessionId != null) {
                sessionUserMap.remove(oldSessionId);
            }
        }
        
        onlineUsers.add(username);
        userSessionMap.put(username, sessionId);
        sessionUserMap.put(sessionId, username);
        log.info("用户上线: {}, sessionId={}, 当前在线人数: {}", username, sessionId, onlineUsers.size());
    }
    
    @Override
    public void userOnline(String username) {
        onlineUsers.add(username);
        log.info("用户上线: {}, 当前在线人数: {}", username, onlineUsers.size());
    }
    
    @Override
    public void userOffline(String username) {
        String sessionId = userSessionMap.remove(username);
        if (sessionId != null) {
            sessionUserMap.remove(sessionId);
        }
        onlineUsers.remove(username);
        userBattleStatus.remove(username);
        log.info("用户下线: {}, 当前在线人数: {}", username, onlineUsers.size());
    }
    
    @Override
    public boolean isUserOnline(String username) {
        return onlineUsers.contains(username);
    }
    
    @Override
    public Set<String> getAllOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }
    
    @Override
    public void enterBattle(String username, String roomCode) {
        userBattleStatus.put(username, roomCode);
        log.info("用户进入对战: {}, 房间: {}", username, roomCode);
    }
    
    @Override
    public void leaveBattle(String username) {
        String roomCode = userBattleStatus.remove(username);
        log.info("用户退出对战: {}, 房间: {}", username, roomCode);
    }
    
    @Override
    public boolean isUserInBattle(String username) {
        return userBattleStatus.containsKey(username);
    }
    
    @Override
    public String getUserRoomCode(String username) {
        return userBattleStatus.get(username);
    }
    
    @Override
    public String getUserSessionId(String username) {
        return userSessionMap.get(username);
    }
    
    @Override
    public String getUsernameBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
}
