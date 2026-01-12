package com.campusguess.demo.model.dto.user;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserInfoResponse {
    private Long userId;
    private String username;
    private String role;
    private LocalDateTime createdAt;
    private Integer points;

    public UserInfoResponse(Long userId, String username, String role, LocalDateTime createdAt, Integer points) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.points = points;
    }
}