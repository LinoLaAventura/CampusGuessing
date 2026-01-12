package com.campusguess.demo.controller;

import com.campusguess.demo.model.dto.comment.CommentRequest;
import com.campusguess.demo.model.dto.comment.CommentResponse;
import com.campusguess.demo.model.dto.response.ApiResponse;
import com.campusguess.demo.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 获取题目评论列表
    @GetMapping("/questions/{questionId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long questionId) {
        
        List<CommentResponse> comments = commentService.getCommentsByQuestionId(questionId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", comments));
    }

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

    // 5.3 いいね取り消し
    @DeleteMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<Void>> unlikeComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        
        commentService.unlikeComment(commentId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("取消点赞成功", null));
    }

    // 5.4 コメント削除 (★ここを追加しました！)
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        
        commentService.deleteComment(commentId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("评论删除成功", null));
    }
}