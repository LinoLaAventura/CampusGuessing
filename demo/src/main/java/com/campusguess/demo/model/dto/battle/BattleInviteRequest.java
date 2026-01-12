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
}
