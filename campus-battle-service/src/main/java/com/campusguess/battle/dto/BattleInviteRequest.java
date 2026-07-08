package com.campusguess.battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送对战邀请请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleInviteRequest {

    /** 邀请方用户名 */
    private String inviter;

    /** 被邀请方用户名 */
    private String invitee;

    /** 游戏模式 */
    private String gameType;
}