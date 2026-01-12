package com.campusguess.demo.model.dto.battle;

import com.campusguess.demo.model.dto.question.QuestionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对战状态消息（推送给双方）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleStateMessage {
    
    /** 消息类型 */
    private MessageType type;
    
    /** 房间代码 */
    private String roomCode;
    
    /** 当前题目 */
    private QuestionResponse question;
    
    /** 玩家A用户名 */
    private String playerA;
    
    /** 玩家B用户名 */
    private String playerB;
    
    /** 玩家A血量 */
    private Integer playerAHealth;
    
    /** 玩家B血量 */
    private Integer playerBHealth;
    
    /** 当前回合 */
    private Integer currentRound;
    
    /** 玩家A是否已作答 */
    private Boolean playerAAnswered;
    
    /** 玩家B是否已作答 */
    private Boolean playerBAnswered;
    
    /** 胜利者（游戏结束时） */
    private String winner;
    
    /** 附加消息 */
    private String message;
    
    /** 倒计时秒数（仅在等待对方作答时） */
    private Integer countdown;
    
    /** 回合结果详情 */
    private RoundResult roundResult;
    
    public enum MessageType {
        INVITE,          // 邀请通知
        INVITE_ACCEPTED, // 邀请被接受
        INVITE_REJECTED, // 邀请被拒绝
        GAME_START,      // 游戏开始
        NEW_QUESTION,    // 新题目
        PLAYER_ANSWERED, // 玩家已作答
        ROUND_RESULT,    // 回合结果
        GAME_OVER        // 游戏结束
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundResult {
        /** 玩家A距离正确答案的距离（米） */
        private Double playerADistance;
        
        /** 玩家B距离正确答案的距离（米） */
        private Double playerBDistance;
        
        /** 扣血的玩家 */
        private String damagedPlayer;
        
        /** 扣除的血量 */
        private Integer damage;
    }
}
