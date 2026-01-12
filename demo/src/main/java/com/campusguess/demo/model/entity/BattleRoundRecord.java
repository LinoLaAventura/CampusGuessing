package com.campusguess.demo.model.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 对战回合记录
 * 用于存储每个回合的详细信息
 */
@Entity
@Table(name = "battle_round_records")
@Data
public class BattleRoundRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 关联的对战房间 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_room_id", nullable = false)
    private BattleRoom battleRoom;
    
    /** 回合数 */
    @Column(nullable = false)
    private Integer roundNumber;
    
    /** 题目ID */
    @Column(nullable = false)
    private Long questionId;
    
    /** 玩家A的经度 */
    @Column(name = "player_a_lon")
    private Double playerALon;
    
    /** 玩家A的纬度 */
    @Column(name = "player_a_lat")
    private Double playerALat;
    
    /** 玩家B的经度 */
    @Column(name = "player_b_lon")
    private Double playerBLon;
    
    /** 玩家B的纬度 */
    @Column(name = "player_b_lat")
    private Double playerBLat;
    
    /** 玩家A的距离（米） */
    @Column(name = "player_a_distance")
    private Double playerADistance;
    
    /** 玩家B的距离（米） */
    @Column(name = "player_b_distance")
    private Double playerBDistance;
    
    /** 被扣血的玩家 (playerA 或 playerB) */
    @Column(name = "damaged_player")
    private String damagedPlayer;
    
    /** 扣除的血量 */
    private Integer damage;
    
    /** 回合后玩家A的血量 */
    @Column(name = "player_a_health_after")
    private Integer playerAHealthAfter;
    
    /** 回合后玩家B的血量 */
    @Column(name = "player_b_health_after")
    private Integer playerBHealthAfter;
}
