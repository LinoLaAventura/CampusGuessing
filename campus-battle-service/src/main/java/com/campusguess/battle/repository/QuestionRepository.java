package com.campusguess.battle.repository;

import com.campusguess.battle.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * 随机获取一道题目（数据库层面随机，避免全量加载）
     * 使用 MySQL ORDER BY RAND() LIMIT 1
     */
    @Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestion();
}