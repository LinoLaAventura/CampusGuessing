package com.campusguess.battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交答案请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleAnswerRequest {

    /** 房间代码 */
    private String roomCode;

    /** 提交者用户名 */
    private String username;

    /** 经度 */
    private Double lon;

    /** 纬度 */
    private Double lat;
}