package com.campusguess.demo.model.dto.question;

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
    private String imageKey; // 图片key
    private CorrectCoord correctCoord;
    private Object imageData; // 从图床获取到的图片信息对象

    @Data
    public static class CorrectCoord {
        private Double lon;
        private Double lat;
    }
}
