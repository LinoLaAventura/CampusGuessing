package com.campusguess.question.service.impl;

import com.campusguess.common.exception.BusinessException;
import com.campusguess.question.dto.CreateQuestionRequest;
import com.campusguess.question.dto.QuestionListResponse;
import com.campusguess.question.dto.QuestionResponse;
import com.campusguess.question.entity.Question;
import com.campusguess.common.entity.User;
import com.campusguess.question.repository.QuestionRepository;
import com.campusguess.question.repository.UserRepository;
import com.campusguess.question.service.ImageClient;
import com.campusguess.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ImageClient imageClient;

    @Override
    @Transactional
    public QuestionResponse createQuestion(String username, CreateQuestionRequest request) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setAnswer(request.getAnswer());
        question.setCampus(request.getCampus());
        question.setDifficulty(request.getDifficulty());
        question.setImageKey(request.getKey());
        question.setAuthor(author);

        if (request.getCorrectCoord() != null) {
            question.setCorrectLon(request.getCorrectCoord().getLon());
            question.setCorrectLat(request.getCorrectCoord().getLat());
        }

        question = questionRepository.save(question);
        return toResponse(question, null);
    }

    @Override
    public QuestionResponse getQuestion(Long id) {
        Question question = questionRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new BusinessException("题目不存在"));

        Map<String, Object> imageData = null;
        if (question.getImageKey() != null && !question.getImageKey().isEmpty()) {
            imageData = imageClient.fetchImageByKey(question.getImageKey());
        }
        return toResponse(question, imageData);
    }

    @Override
    public QuestionListResponse listQuestions(Pageable pageable) {
        Page<Question> page = questionRepository.findAll(pageable);
        List<QuestionResponse> list = page.getContent().stream()
                .map(q -> toResponse(q, null))
                .collect(Collectors.toList());
        return new QuestionListResponse(page.getTotalElements(), list);
    }

    @Override
    public QuestionListResponse listByUser(String username, Pageable pageable) {
        Page<Question> page = questionRepository.findByAuthorUsername(username, pageable);
        List<QuestionResponse> list = page.getContent().stream()
                .map(q -> toResponse(q, null))
                .collect(Collectors.toList());
        return new QuestionListResponse(page.getTotalElements(), list);
    }

    @Override
    @Transactional
    public void deleteQuestion(String username, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("题目不存在"));

        if (!question.getAuthor().getUsername().equals(username)) {
            throw new BusinessException("无权删除他人题目");
        }
        questionRepository.delete(question);
    }

    private QuestionResponse toResponse(Question q, Object imageData) {
        QuestionResponse resp = new QuestionResponse();
        resp.setId(q.getId());
        resp.setTitle(q.getTitle());
        resp.setContent(q.getContent());
        resp.setAnswer(q.getAnswer());
        resp.setCampus(q.getCampus());
        resp.setDifficulty(q.getDifficulty());
        resp.setImageKey(q.getImageKey());
        resp.setCreatedAt(q.getCreatedAt());
        resp.setImageData(imageData);

        if (q.getAuthor() != null) {
            resp.setAuthorId(q.getAuthor().getId());
            resp.setAuthorUsername(q.getAuthor().getUsername());
        }

        if (q.getCorrectLon() != null || q.getCorrectLat() != null) {
            QuestionResponse.CorrectCoord coord = new QuestionResponse.CorrectCoord();
            coord.setLon(q.getCorrectLon());
            coord.setLat(q.getCorrectLat());
            resp.setCorrectCoord(coord);
        }

        return resp;
    }
}