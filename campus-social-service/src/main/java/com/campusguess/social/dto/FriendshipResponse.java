package com.campusguess.social.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FriendshipResponse {
    private Long friendshipId;
    private Long applicantId;
    private String applicantUsername;
    private Long receiverId;
    private String receiverUsername;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime handledAt;
    private String handledType;
}