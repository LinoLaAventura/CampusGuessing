package com.campusguess.social.repository;

import com.campusguess.social.entity.Comment;
import com.campusguess.social.entity.CommentLike;
import com.campusguess.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByUserAndComment(User user, Comment comment);
    boolean existsByUserAndComment(User user, Comment comment);
    long countByComment(Comment comment);
}