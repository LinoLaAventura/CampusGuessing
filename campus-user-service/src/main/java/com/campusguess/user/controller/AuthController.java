package com.campusguess.user.controller;

import com.campusguess.common.response.ApiResponse;
import com.campusguess.common.dto.UserInfoResponse;
import com.campusguess.user.config.JwtTokenUtil;
import com.campusguess.user.dto.auth.LoginRequest;
import com.campusguess.user.dto.auth.LoginResponse;
import com.campusguess.common.entity.User;
import com.campusguess.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request);

        String token = jwtTokenUtil.generateToken(user);
        Date expireDate = jwtTokenUtil.getExpirationDateFromToken(token);

        userService.updateLastLogin(user.getId());

        UserInfoResponse userInfo = new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getPoints());

        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .expireTime(LocalDateTime.ofInstant(expireDate.toInstant(), ZoneId.systemDefault()))
                .userInfo(userInfo)
                .build();

        return ResponseEntity.ok(ApiResponse.success("登录成功", loginResponse));
    }
}