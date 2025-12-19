package com.campusguess.demo.controller;

import com.campusguess.demo.model.dto.comment.CommentRequest;
import com.campusguess.demo.model.dto.comment.CommentResponse;
import com.campusguess.demo.model.dto.response.ApiResponse;
import com.campusguess.demo.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 5.1 コメント投稿
    @PostMapping("/questions/{questionId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long questionId,
            @Valid @RequestBody CommentRequest request) {
        
        CommentResponse response = commentService.addComment(questionId, request);
        return ResponseEntity.status(201).body(ApiResponse.created("评论发表成功", response));
    }

    // 5.2 いいね
    @PostMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<Void>> likeComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) { // userId取得用にRequest再利用
        
        commentService.likeComment(commentId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("点赞成功", null));
    }

    @DeleteMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<Void>> unlikeComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        
        commentService.unlikeComment(commentId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("取消点赞成功", null));
    }
}