package com.campusguess.social.repository;

import com.campusguess.social.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByQuestionIdOrderByCreatedAtDesc(Long questionId);
    long countByQuestionId(Long questionId);
}