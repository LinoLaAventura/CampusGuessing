package com.campusguess.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class HandleFriendRequest {
    @NotBlank(message = "处理类型不能为空")
    @Pattern(regexp = "accept|reject", message = "处理类型必须是accept或reject")
    private String handleType;

    @NotBlank(message = "friendUsername不能为空")
    private String friendUsername;
}