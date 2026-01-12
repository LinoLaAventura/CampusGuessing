package com.campusguess.demo.service;

import com.campusguess.demo.model.dto.comment.CommentRequest;
import com.campusguess.demo.model.dto.comment.CommentResponse;

import java.util.List;

public interface CommentService {
    // 获取题目评论列表
    List<CommentResponse> getCommentsByQuestionId(Long questionId);

    // コメント投稿
    CommentResponse addComment(Long questionId, CommentRequest request);

    // いいね
    void likeComment(Long commentId, Long userId);

    // いいね解除
    void unlikeComment(Long commentId, Long userId);

    // 5.4 コメント削除 
    void deleteComment(Long commentId, Long userId);
}