package com.campusguess.question.controller;

import com.campusguess.common.response.ApiResponse;
import com.campusguess.question.dto.CreateQuestionRequest;
import com.campusguess.question.dto.QuestionListResponse;
import com.campusguess.question.dto.QuestionResponse;
import com.campusguess.question.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping("/users/{username}/questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @PathVariable String username,
            @Valid @RequestBody CreateQuestionRequest request) {
        QuestionResponse resp = questionService.createQuestion(username, request);
        return ResponseEntity.ok(ApiResponse.success("题目创建成功", resp));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestion(@PathVariable Long id) {
        QuestionResponse resp = questionService.getQuestion(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", resp));
    }

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<QuestionListResponse>> listQuestions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        QuestionListResponse resp = questionService.listQuestions(pageable);
        return ResponseEntity.ok(ApiResponse.success("查询成功", resp));
    }

    @GetMapping("/users/{username}/questions")
    public ResponseEntity<ApiResponse<QuestionListResponse>> listByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        QuestionListResponse resp = questionService.listByUser(username, pageable);
        return ResponseEntity.ok(ApiResponse.success("查询成功", resp));
    }

    @DeleteMapping("/users/{username}/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable String username,
            @PathVariable Long questionId) {
        questionService.deleteQuestion(username, questionId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}