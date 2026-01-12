package com.campusguess.demo.repository;

import com.campusguess.demo.model.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByQuestionId(Long questionId, Pageable pageable);
    
    List<Comment> findByQuestionIdOrderByCreatedAtDesc(Long questionId);
}