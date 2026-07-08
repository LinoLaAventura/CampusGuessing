package com.campusguess.battle.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题目实体（只读，battle service 用于随机获取题目）
 */
@Entity
@Table(name = "questions")
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_key")
    private String imageKey;

    @Column(nullable = false)
    private Double lon;

    @Column(nullable = false)
    private Double lat;

    @Column(name = "campus", length = 100)
    private String campus;

    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}