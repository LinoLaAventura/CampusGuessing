package com.campusguess.social.controller;

import com.campusguess.common.response.ApiResponse;
import com.campusguess.social.dto.CommentRequest;
import com.campusguess.social.dto.CommentResponse;
import com.campusguess.social.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/question/{questionId}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long questionId) {
        List<CommentResponse> result = commentService.getCommentsByQuestionId(questionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/question/{questionId}")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long questionId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse result = commentService.addComment(questionId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Void>> likeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.likeComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.unlikeComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}