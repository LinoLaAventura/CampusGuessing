package com.campusguess.question.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuestionListResponse {
    private Long total;
    private List<QuestionResponse> list;
}