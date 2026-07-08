package com.campusguess.social.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FriendResponse {
    private Long friendId;
    private String friendUsername;
    private Integer friendPoints;
    private String friendshipStatus;
    private LocalDateTime lastActiveTime;
    private LocalDateTime becameFriendAt;

    public FriendResponse(Long friendId, String friendUsername, Integer friendPoints,
            String friendshipStatus, LocalDateTime lastActiveTime, LocalDateTime becameFriendAt) {
        this.friendId = friendId;
        this.friendUsername = friendUsername;
        this.friendPoints = friendPoints;
        this.friendshipStatus = friendshipStatus;
        this.lastActiveTime = lastActiveTime;
        this.becameFriendAt = becameFriendAt;
    }
}