package com.campusguess.question.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionResponse {
    private Long id;
    private String title;
    private String content;
    private String answer;
    private Long authorId;
    private String authorUsername;
    private LocalDateTime createdAt;
    private String campus;
    private String difficulty;
    private String imageKey;
    private CorrectCoord correctCoord;
    private Object imageData;

    @Data
    public static class CorrectCoord {
        private Double lon;
        private Double lat;
    }
}