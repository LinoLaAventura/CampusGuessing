package com.campusguess.demo.controller;

import com.campusguess.demo.model.dto.auth.RegisterRequest;
import com.campusguess.demo.model.entity.User;
import com.campusguess.demo.model.dto.response.ApiResponse;
import com.campusguess.demo.model.dto.user.UserInfoResponse;
import com.campusguess.demo.model.dto.user.PointChangeRequest;
import com.campusguess.demo.model.dto.user.PointChangeResponse;
import com.campusguess.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserInfoResponse>> register(@Valid @RequestBody RegisterRequest request) {
        // 注册用户
        User user = userService.register(request);

        // 构建响应
        UserInfoResponse userInfo = new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getPoints());

        return ResponseEntity.status(201).body(ApiResponse.created("注册成功", userInfo));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(
            @PathVariable("username") String username) {

        User user = userService.findByUsername(username);
        UserInfoResponse userInfo = new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getPoints());

        return ResponseEntity.ok(ApiResponse.success("查询成功", userInfo));
    }

    @PutMapping("/{username}/changePoint")
    public ResponseEntity<ApiResponse<PointChangeResponse>> changeUserPoints(
            @PathVariable("username") String username,
            @Valid @RequestBody PointChangeRequest request) {

        PointChangeResponse resp = userService.changePoints(username, request.getPointChange());
        return ResponseEntity.ok(ApiResponse.success("用户积分修改成功", resp));
    }
}