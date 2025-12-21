package com.campusguess.demo.repository;

import com.campusguess.demo.model.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByUser_IdAndComment_Id(Long userId, Long commentId);
    void deleteByUser_IdAndComment_Id(Long userId, Long commentId);

    /** 删除指定评论的所有点赞，用于评论删除级联清理 */
    void deleteByComment_Id(Long commentId);
}