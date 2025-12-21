package com.campusguess.demo.service.impl;

import com.campusguess.demo.exception.BusinessException;
import com.campusguess.demo.model.dto.comment.CommentRequest;
import com.campusguess.demo.model.dto.comment.CommentResponse;
import com.campusguess.demo.model.entity.*;
import com.campusguess.demo.repository.*;
import com.campusguess.demo.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentResponse addComment(Long questionId, CommentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setQuestion(question);
        comment.setContent(request.getContent());
        comment.setLikeCount(0);

        Comment saved = commentRepository.save(comment);

        // レスポンス作成
        CommentResponse response = new CommentResponse();
        response.setCommentId(saved.getId());
        response.setContent(saved.getContent());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setCreateTime(saved.getCreatedAt());
        response.setLikeCount(saved.getLikeCount());

        return response;
    }

    @Override
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 重複チェック
        if (commentLikeRepository.existsByUser_IdAndComment_Id(userId, commentId)) {
            throw new BusinessException(400, "已点赞");
        }

        CommentLike like = new CommentLike();
        like.setComment(comment);
        like.setUser(user);
        commentLikeRepository.save(like);

        // カウントアップ
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));

        if (!commentLikeRepository.existsByUser_IdAndComment_Id(userId, commentId)) {
            throw new BusinessException(400, "未点赞");
        }

        commentLikeRepository.deleteByUser_IdAndComment_Id(userId, commentId);

        // カウントダウン
        int count = comment.getLikeCount() > 0 ? comment.getLikeCount() - 1 : 0;
        comment.setLikeCount(count);
        commentRepository.save(comment);
    }

    // ★追加: コメント削除機能
    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));

        // 自分のコメントでなければ削除できない (権限チェック)
        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的评论");
        }

        commentRepository.delete(comment);
    }
}