package com.campusguess.record.entity;

import com.campusguess.common.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
@Data
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "image_key")
    private String imageKey;

    @Column(length = 100)
    private String campus;

    @Column(length = 50)
    private String difficulty;

    @Column(name = "correct_lon")
    private Double correctLon;

    @Column(name = "correct_lat")
    private Double correctLat;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}