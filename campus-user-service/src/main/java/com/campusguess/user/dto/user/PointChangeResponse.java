package com.campusguess.user.dto.user;

import lombok.Data;

@Data
public class PointChangeResponse {
    private Long userId;
    private String username;
    private Integer pointBefore;
    private Integer pointChange;
    private Integer pointAfter;

    public PointChangeResponse(Long userId, String username, Integer pointBefore, Integer pointChange, Integer pointAfter) {
        this.userId = userId;
        this.username = username;
        this.pointBefore = pointBefore;
        this.pointChange = pointChange;
        this.pointAfter = pointAfter;
    }
}