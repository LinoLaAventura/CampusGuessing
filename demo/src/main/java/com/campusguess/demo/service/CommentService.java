package com.campusguess.demo.service;

import com.campusguess.demo.model.dto.comment.CommentRequest;
import com.campusguess.demo.model.dto.comment.CommentResponse;

public interface CommentService {
    // コメント投稿
    CommentResponse addComment(Long questionId, CommentRequest request);

    // いいね
    void likeComment(Long commentId, Long userId);

    // いいね解除
    void unlikeComment(Long commentId, Long userId);

    // 5.4 コメント削除 
    void deleteComment(Long commentId, Long userId);
}