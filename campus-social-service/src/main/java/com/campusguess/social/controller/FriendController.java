package com.campusguess.social.controller;

import com.campusguess.common.response.ApiResponse;
import com.campusguess.social.dto.*;
import com.campusguess.social.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<FriendshipResponse>> addFriend(
            @RequestParam String username,
            @Valid @RequestBody AddFriendRequest request) {
        FriendshipResponse result = friendService.addFriend(username, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/handle/{friendName}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> handleFriendRequest(
            @RequestParam String username,
            @PathVariable String friendName,
            @Valid @RequestBody HandleFriendRequest request) {
        FriendshipResponse result = friendService.handleFriendRequest(username, friendName, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<FriendListResponse>> getFriendList(
            @RequestParam String username,
            @PageableDefault(size = 20) Pageable pageable) {
        FriendListResponse result = friendService.getFriendList(username, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<FriendListResponse>> getPendingRequests(
            @RequestParam String username,
            @PageableDefault(size = 20) Pageable pageable) {
        FriendListResponse result = friendService.getPendingRequests(username, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<FriendListResponse>> getSentRequests(
            @RequestParam String username,
            @PageableDefault(size = 20) Pageable pageable) {
        FriendListResponse result = friendService.getSentRequests(username, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkIsFriend(
            @RequestParam String username,
            @RequestParam String friendUsername) {
        boolean result = friendService.isFriend(username, friendUsername);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/remove/{friendUsername}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @RequestParam String username,
            @PathVariable String friendUsername) {
        friendService.removeFriend(username, friendUsername);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/cancel/{friendshipId}")
    public ResponseEntity<ApiResponse<Void>> cancelFriendRequest(
            @RequestParam String username,
            @PathVariable Long friendshipId) {
        friendService.cancelFriendRequest(username, friendshipId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getFriendCount(@RequestParam String username) {
        Long count = friendService.getFriendCount(username);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}