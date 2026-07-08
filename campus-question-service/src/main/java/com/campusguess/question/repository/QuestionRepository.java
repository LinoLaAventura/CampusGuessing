package com.campusguess.question.repository;

import com.campusguess.question.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Page<Question> findByAuthorId(Long authorId, Pageable pageable);
    Page<Question> findByAuthorUsername(String username, Pageable pageable);

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.author WHERE q.id = :id")
    Optional<Question> findByIdWithAuthor(@Param("id") Long id);

    @Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestion();
}