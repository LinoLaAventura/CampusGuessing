package com.campusguess.question.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateQuestionRequest {
    @NotBlank(message = "campus 不能为空")
    private String campus;

    @NotBlank(message = "difficulty 不能为空")
    private String difficulty;

    @NotBlank(message = "key 不能为空")
    private String key;

    private CorrectCoord correctCoord;

    private String title;
    private String content;
    private String answer;

    @Data
    public static class CorrectCoord {
        private Double lon;
        private Double lat;
    }
}