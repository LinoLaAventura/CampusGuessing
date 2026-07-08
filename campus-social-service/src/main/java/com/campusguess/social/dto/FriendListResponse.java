package com.campusguess.social.dto;

import lombok.Data;
import java.util.List;

@Data
public class FriendListResponse {
    private Long total;
    private List<FriendResponse> friendList;

    public FriendListResponse(Long total, List<FriendResponse> friendList) {
        this.total = total;
        this.friendList = friendList;
    }
}