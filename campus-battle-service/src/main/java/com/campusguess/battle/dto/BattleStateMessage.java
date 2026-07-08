package com.campusguess.battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对战状态消息（通用WebSocket推送消息）
 * 用于向前端推送各种对战状态变化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleStateMessage {

    /** 消息类型 */
    private MessageType type;

    /** 对战状态 */
    private BattleStatus status;

    /** 房间代码 */
    private String roomCode;

    /** 玩家A用户名 */
    private String playerA;

    /** 玩家B用户名 */
    private String playerB;

    /** 玩家A当前血量 */
    private Integer playerAHealth;

    /** 玩家B当前血量 */
    private Integer playerBHealth;

    /** 当前回合数 */
    private Integer currentRound;

    /** 玩家A是否已作答 */
    private Boolean playerAAnswered;

    /** 玩家B是否已作答 */
    private Boolean playerBAnswered;

    /** 获胜者用户名 */
    private String winner;

    /** 游戏模式 */
    private String gameType;

    /** 当前题目ID */
    private Long questionId;

    /** 当前题目图片URL */
    private String questionImageUrl;

    /** 当前题目信息 */
    private QuestionResponse question;

    /** 回合结果详情 */
    private RoundResult roundResult;

    /** 倒计时（秒） */
    private Integer countdown;

    /** 消息文本 */
    private String message;

    public enum MessageType {
        INVITE,           // 邀请对战
        INVITE_REJECTED,  // 邀请被拒绝
        GAME_START,       // 游戏开始
        NEW_QUESTION,     // 新题目
        PLAYER_ANSWERED,  // 玩家已作答
        ROUND_RESULT,     // 回合结果
        GAME_OVER         // 游戏结束
    }

    public enum BattleStatus {
        WAITING,         // 等待对手接受
        REJECTED,        // 对方拒绝
        PLAYING,         // 对战中
        WAITING_ANSWER,  // 等待对方作答
        FINISHED         // 对战结束
    }

    /**
     * 回合结果详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundResult {
        /** 回合数 */
        private Integer roundNumber;

        /** 题目ID */
        private Long questionId;

        /** 玩家A的经度 */
        private Double playerALon;

        /** 玩家A的纬度 */
        private Double playerALat;

        /** 玩家B的经度 */
        private Double playerBLon;

        /** 玩家B的纬度 */
        private Double playerBLat;

        /** 玩家A的距离（米） */
        private Double playerADistance;

        /** 玩家B的距离（米） */
        private Double playerBDistance;

        /** 被扣血的玩家 */
        private String damagedPlayer;

        /** 扣除的血量 */
        private Integer damage;

        /** 正确坐标 */
        private CorrectCoord correctCoord;

        /** 回合后玩家A的血量 */
        private Integer playerAHealthAfter;

        /** 回合后玩家B的血量 */
        private Integer playerBHealthAfter;
    }

    /**
     * 正确坐标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrectCoord {
        private Double lon;
        private Double lat;
    }

    /**
     * 题目响应（精简版，用于WebSocket推送）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResponse {
        private Long id;
        private String title;
        private String content;
        private String imageKey;
        private String campus;
        private String difficulty;
        private CorrectCoord correctCoord;
    }
}