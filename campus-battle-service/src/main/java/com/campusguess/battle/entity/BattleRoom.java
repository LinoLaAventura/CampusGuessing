package com.campusguess.battle.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对战房间实体
 * 记录每场对战的详细信息
 */
@Entity
@Table(name = "battle_rooms")
@Data
public class BattleRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 房间唯一标识（用于WebSocket通信） */
    @Column(nullable = false, unique = true)
    private String roomCode;

    /** 玩家A的用户名 */
    @Column(nullable = false)
    private String playerA;

    /** 玩家B的用户名 */
    @Column(nullable = false)
    private String playerB;

    /** 玩家A当前血量 */
    @Column(nullable = false)
    private Integer playerAHealth = 100;

    /** 玩家B当前血量 */
    @Column(nullable = false)
    private Integer playerBHealth = 100;

    /** 当前题目ID */
    private Long currentQuestionId;

    /** 当前回合数 */
    @Column(nullable = false)
    private Integer currentRound = 1;

    /** 玩家A是否已作答 */
    @Column(nullable = false)
    private Boolean playerAAnswered = false;

    /** 玩家B是否已作答 */
    @Column(nullable = false)
    private Boolean playerBAnswered = false;

    /** 玩家A的答案（经纬度JSON） */
    private String playerAAnswer;

    /** 玩家B的答案（经纬度JSON） */
    private String playerBAnswer;

    /** 房间状态：WAITING(等待接受), PLAYING(进行中), FINISHED(已结束) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BattleStatus status = BattleStatus.WAITING;

    /** 胜利者用户名 */
    private String winner;

    /** 创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime finishedAt;

    /** 回合历史数据（JSON格式存储） */
    @Column(columnDefinition = "TEXT")
    private String roundHistoryJson;

    /** 游戏模式：独自变强、好友对战、积分排行 */
    @Column(name = "game_type", length = 20)
    private String gameType;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum BattleStatus {
        WAITING,   // 等待玩家B接受邀请
        PLAYING,   // 对战进行中
        FINISHED   // 对战已结束
    }
}