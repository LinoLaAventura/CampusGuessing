package com.campusguess.demo.repository;

import com.campusguess.demo.model.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByUser_IdAndComment_Id(Long userId, Long commentId);
    void deleteByUser_IdAndComment_Id(Long userId, Long commentId);
}