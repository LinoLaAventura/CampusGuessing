package com.campusguess.user.service;

import com.campusguess.common.entity.User;
import com.campusguess.user.dto.auth.LoginRequest;
import com.campusguess.user.dto.auth.RegisterRequest;
import com.campusguess.user.dto.user.PointChangeResponse;

public interface UserService {
    User register(RegisterRequest request);

    User authenticate(LoginRequest request);

    User findByUsername(String username);

    User findById(Long id);

    void updateLastLogin(Long userId);

    PointChangeResponse changePoints(String username, Integer pointChange);
}