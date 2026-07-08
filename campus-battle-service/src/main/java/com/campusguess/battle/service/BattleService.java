package com.campusguess.battle.service;

import com.campusguess.battle.dto.BattleAnswerRequest;
import com.campusguess.battle.dto.BattleInviteRequest;
import com.campusguess.battle.dto.BattleInviteResponse;
import com.campusguess.battle.dto.BattleStateMessage;

/**
 * 对战核心服务接口
 */
public interface BattleService {

    /**
     * 发送对战邀请
     */
    BattleStateMessage sendInvite(BattleInviteRequest request);

    /**
     * 响应邀请（接受/拒绝）
     */
    BattleStateMessage handleInviteResponse(BattleInviteResponse response);

    /**
     * 提交答案
     */
    BattleStateMessage submitAnswer(BattleAnswerRequest request);
}