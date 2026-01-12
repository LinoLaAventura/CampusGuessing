package com.campusguess.demo.model.dto.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家作答请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleAnswerRequest {
    
    /** 房间代码 */
    private String roomCode;
    
    /** 作答者用户名 */
    private String username;
    
    /** 答案经度 */
    private Double longitude;
    
    /** 答案纬度 */
    private Double latitude;
}
