package com.campusguess.question.service;

import com.campusguess.question.dto.CreateQuestionRequest;
import com.campusguess.question.dto.QuestionListResponse;
import com.campusguess.question.dto.QuestionResponse;
import org.springframework.data.domain.Pageable;

public interface QuestionService {
    QuestionResponse createQuestion(String username, CreateQuestionRequest request);

    QuestionResponse getQuestion(Long id);

    QuestionListResponse listQuestions(Pageable pageable);

    QuestionListResponse listByUser(String username, Pageable pageable);

    void deleteQuestion(String username, Long questionId);
}