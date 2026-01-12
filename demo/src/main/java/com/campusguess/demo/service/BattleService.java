package com.campusguess.demo.service;

import com.campusguess.demo.model.dto.battle.*;
import com.campusguess.demo.model.entity.BattleRoom;

/**
 * 对战服务接口
 */
public interface BattleService {

    /**
     * 创建对战邀请
     */
    BattleRoom createInvite(String fromUsername, String toUsername);

    /**
     * 接受对战邀请
     */
    BattleRoom acceptInvite(String roomCode, String username);

    /**
     * 拒绝对战邀请
     */
    void rejectInvite(String roomCode, String username);

    /**
     * 提交答案
     */
    BattleRoom submitAnswer(String roomCode, String username, Double longitude, Double latitude);

    /**
     * 获取房间信息
     */
    BattleRoom getRoomByCode(String roomCode);

    /**
     * 计算回合结果（当双方都作答后）
     */
    BattleStateMessage.RoundResult calculateRoundResult(BattleRoom room);

    /**
     * 开始新回合
     */
    BattleRoom startNewRound(String roomCode);

    /**
     * 检查游戏是否结束
     */
    boolean isGameOver(BattleRoom room);
    
    /**
     * 保存对战结果到记录表
     */
    void saveBattleRecords(BattleRoom room);
}
