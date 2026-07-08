package com.campusguess.social.service.impl;

import com.campusguess.common.exception.BusinessException;
import com.campusguess.social.dto.CommentRequest;
import com.campusguess.social.dto.CommentResponse;
import com.campusguess.social.entity.Comment;
import com.campusguess.social.entity.CommentLike;
import com.campusguess.common.entity.User;
import com.campusguess.social.repository.CommentLikeRepository;
import com.campusguess.social.repository.CommentRepository;
import com.campusguess.social.repository.UserRepository;
import com.campusguess.social.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    @Override
    public List<CommentResponse> getCommentsByQuestionId(Long questionId) {
        List<Comment> comments = commentRepository.findByQuestionIdOrderByCreatedAtDesc(questionId);

        return comments.stream().map(comment -> {
            CommentResponse response = new CommentResponse();
            response.setCommentId(comment.getId());
            response.setContent(comment.getContent());
            response.setUserId(comment.getUser().getId());
            response.setUsername(comment.getUser().getUsername());
            response.setCreateTime(comment.getCreatedAt());
            response.setLikeCount(comment.getLikeCount());
            return response;
        }).toList();
    }

    @Override
    @Transactional
    public CommentResponse addComment(Long questionId, CommentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setQuestionId(questionId);
        comment.setContent(request.getContent());
        comment.setLikeCount(0);

        Comment saved = commentRepository.save(comment);

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

        if (commentLikeRepository.existsByUserAndComment(user, comment)) {
            throw new BusinessException(400, "已点赞");
        }

        CommentLike like = new CommentLike();
        like.setComment(comment);
        like.setUser(user);
        commentLikeRepository.save(like);

        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        CommentLike like = commentLikeRepository.findByUserAndComment(user, comment)
                .orElseThrow(() -> new BusinessException(400, "未点赞"));

        commentLikeRepository.delete(like);

        int count = comment.getLikeCount() > 0 ? comment.getLikeCount() - 1 : 0;
        comment.setLikeCount(count);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的评论");
        }

        commentRepository.delete(comment);
    }
}