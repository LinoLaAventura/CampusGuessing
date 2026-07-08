package com.campusguess.social.service;

import com.campusguess.social.dto.CommentRequest;
import com.campusguess.social.dto.CommentResponse;

import java.util.List;

public interface CommentService {
    List<CommentResponse> getCommentsByQuestionId(Long questionId);
    CommentResponse addComment(Long questionId, CommentRequest request);
    void likeComment(Long commentId, Long userId);
    void unlikeComment(Long commentId, Long userId);
    void deleteComment(Long commentId, Long userId);
}