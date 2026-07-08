package com.campusguess.user.service.impl;

import com.campusguess.common.exception.BusinessException;
import com.campusguess.user.dto.auth.LoginRequest;
import com.campusguess.user.dto.auth.RegisterRequest;
import com.campusguess.user.dto.user.PointChangeResponse;
import com.campusguess.common.entity.User;
import com.campusguess.user.repository.UserRepository;
import com.campusguess.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : "user");
        user.setPoints(0);

        return userRepository.save(user);
    }

    @Override
    public User authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "账号不存在或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "账号不存在或密码错误");
        }

        if (!user.getEnabled()) {
            throw new BusinessException(401, "账号已被禁用");
        }

        return user;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    @Override
    @Transactional
    public void updateLastLogin(Long userId) {
        User user = findById(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public PointChangeResponse changePoints(String username, Integer pointChange) {
        if (pointChange == null) {
            throw new BusinessException(400, "pointChange不能为空");
        }

        User user = findByUsername(username);
        Integer before = user.getPoints() != null ? user.getPoints() : 0;
        Integer after = before + pointChange;
        user.setPoints(after);
        userRepository.save(user);

        return new PointChangeResponse(user.getId(), user.getUsername(), before, pointChange, after);
    }
}