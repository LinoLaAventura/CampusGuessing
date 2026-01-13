package com.campusguess.demo.model.dto.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请对战请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleInviteRequest {
    
    /** 邀请者用户名 */
    private String fromUsername;
    
    /** 被邀请者用户名 */
    private String toUsername;
    
    /** 游戏模式：独自变强、好友对战、积分排行 */
    private String gameType;
}
