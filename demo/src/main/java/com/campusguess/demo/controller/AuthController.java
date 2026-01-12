package com.campusguess.demo.controller;

import com.campusguess.demo.config.JwtTokenUtil;
import com.campusguess.demo.model.dto.auth.LoginRequest;
import com.campusguess.demo.model.dto.auth.LoginResponse;
import com.campusguess.demo.model.entity.User;
import com.campusguess.demo.model.dto.response.ApiResponse;
import com.campusguess.demo.model.dto.user.UserInfoResponse;
import com.campusguess.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        // 验证用户
        User user = userService.authenticate(request);

        // 生成token
        String token = jwtTokenUtil.generateToken(user);
        Date expireDate = jwtTokenUtil.getExpirationDateFromToken(token);

        // 更新最后登录时间
        userService.updateLastLogin(user.getId());

        // 构建用户信息
        UserInfoResponse userInfo = new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getPoints());

        // 构建响应
        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .expireTime(LocalDateTime.ofInstant(expireDate.toInstant(), ZoneId.systemDefault()))
                .userInfo(userInfo)
                .build();

        return ResponseEntity.ok(ApiResponse.success("登录成功", loginResponse));
    }
}