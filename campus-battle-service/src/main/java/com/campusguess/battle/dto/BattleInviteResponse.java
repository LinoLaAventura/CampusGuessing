package com.campusguess.battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 响应邀请请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleInviteResponse {

    /** 房间代码 */
    private String roomCode;

    /** 响应者用户名 */
    private String username;

    /** 是否接受 */
    private Boolean accepted;
}