package com.campusguess.demo.model.entity;

import lombok.Data;

/**
 * 对战回合记录（临时对象，不持久化到数据库）
 * 用于在内存中暂存每个回合的详细信息
 */
@Data
public class BattleRoundRecord {
    
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
    
    /** 被扣血的玩家 (playerA 或 playerB) */
    private String damagedPlayer;
    
    /** 扣除的血量 */
    private Integer damage;
    
    /** 回合后玩家A的血量 */
    private Integer playerAHealthAfter;
    
    /** 回合后玩家B的血量 */
    private Integer playerBHealthAfter;
}
