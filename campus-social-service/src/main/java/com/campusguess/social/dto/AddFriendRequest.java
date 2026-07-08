package com.campusguess.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddFriendRequest {
    @NotBlank(message = "好友用户名不能为空")
    private String friendUsername;
}