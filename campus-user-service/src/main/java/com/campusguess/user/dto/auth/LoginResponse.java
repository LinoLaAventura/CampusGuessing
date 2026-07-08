package com.campusguess.user.dto.auth;

import com.campusguess.common.dto.UserInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private LocalDateTime expireTime;
    private UserInfoResponse userInfo;
}