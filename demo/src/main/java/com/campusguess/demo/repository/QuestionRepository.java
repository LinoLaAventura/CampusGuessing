package com.campusguess.demo.repository;

import com.campusguess.demo.model.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Page<Question> findByAuthorId(Long authorId, Pageable pageable);
    Page<Question> findByAuthorUsername(String username, Pageable pageable);
    
    /**
     * 获取题目并预加载作者信息（避免LazyInitializationException）
     */
    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.author WHERE q.id = :id")
    Optional<Question> findByIdWithAuthor(@Param("id") Long id);
}
