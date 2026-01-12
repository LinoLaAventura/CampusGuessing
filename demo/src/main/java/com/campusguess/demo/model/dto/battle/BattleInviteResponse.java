package com.campusguess.demo.model.dto.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接受/拒绝对战邀请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleInviteResponse {
    
    /** 房间代码 */
    private String roomCode;
    
    /** 是否接受（true=接受，false=拒绝） */
    private Boolean accepted;
    
    /** 接受/拒绝者用户名 */
    private String username;
}
