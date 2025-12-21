package com.campusguess.demo.service.impl;

import com.campusguess.demo.exception.BusinessException;
import com.campusguess.demo.model.dto.record.*;
import com.campusguess.demo.model.entity.Question;
import com.campusguess.demo.model.entity.Record;
import com.campusguess.demo.model.entity.RecordItem;
import com.campusguess.demo.model.entity.User;
import com.campusguess.demo.repository.QuestionRepository;
import com.campusguess.demo.repository.RecordItemRepository;
import com.campusguess.demo.repository.RecordRepository;
import com.campusguess.demo.repository.UserRepository;
import com.campusguess.demo.service.RecordService;
import com.campusguess.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final RecordItemRepository recordItemRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final UserService userService;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    @Transactional
    public RecordCreateResponse submitRecord(RecordRequest request) {
        if (request == null || request.getUserId() == null || request.getQuestionRecords() == null || request.getQuestionRecords().isEmpty()) {
            throw new BusinessException(400, "参数错误");
        }

        if (request.getGameType() == null || request.getGameType().isBlank()) {
            throw new BusinessException(400, "游戏类型不能为空");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));

        int totalNum = request.getQuestionRecords().size();

        // 计算每题得分并保存 Record + RecordItems
        Record record = new Record();
        record.setUser(user);
        record.setTotalQuestionNum(totalNum);
        record.setGameType(request.getGameType());

        List<RecordItem> items = new ArrayList<>();
        int earnPoints = 0;

        for (QuestionRecordRequest qr : request.getQuestionRecords()) {
            if (qr.getQuestionId() == null || qr.getUserCoord() == null) {
                throw new BusinessException(400, "题目或坐标缺失");
            }

            Question question = questionRepository.findById(qr.getQuestionId())
                    .orElseThrow(() -> new BusinessException(400, "题目ID不存在: " + qr.getQuestionId()));

            double lon = qr.getUserCoord().getLon();
            double lat = qr.getUserCoord().getLat();

            int singleScore = calculateScore(question.getCorrectLon(), question.getCorrectLat(), lon, lat);

            RecordItem item = new RecordItem();
            item.setRecord(record);
            item.setQuestion(question);
            item.setUserLon(lon);
            item.setUserLat(lat);
            item.setSingleScore(singleScore);

            items.add(item);
            earnPoints += singleScore;
        }

        // 保存 record（暂不设置 earnPoints 等，先持久化主表以获取 id）
        record.setEarnPoints(earnPoints);
        int before = user.getPoints() != null ? user.getPoints() : 0;
        record.setPointBefore(before);
        record.setPointAfter(before + earnPoints);

        Record saved = recordRepository.save(record);

        // 关联 items 并保存
        for (RecordItem it : items) {
            it.setRecord(saved);
        }
        recordItemRepository.saveAll(items);

        // 更新用户积分（调用 UserService，以保持一致性）
        userService.changePoints(user.getUsername(), earnPoints);

        return new RecordCreateResponse(saved.getId(), earnPoints);
    }

    @Override
    public List<RecordListItem> getUserRecords(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        List<Record> records = recordRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());

        List<RecordListItem> list = new ArrayList<>();
        for (Record r : records) {
            list.add(new RecordListItem(
                    r.getId(),
                    r.getEarnPoints(),
                    r.getGameType(),
                    r.getCreatedAt() != null ? r.getCreatedAt().format(ISO) : null));
        }

        return list;
    }

    @Override
    public RecordDetailResponse getRecordDetail(Long userId, Long recordId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(404, "记录不存在"));

        if (!record.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权限访问该记录");
        }

        List<RecordDetailResponse.QuestionDetail> qDetails = new ArrayList<>();
        List<RecordItem> items = record.getItems();
        if (items != null) {
            for (RecordItem it : items) {
                Question q = it.getQuestion();
                CoordDTO correct = new CoordDTO();
                correct.setLon(q.getCorrectLon());
                correct.setLat(q.getCorrectLat());

                var base = new RecordDetailResponse.QuestionBase(
                        q.getId(), q.getCampus(), q.getDifficulty(), q.getImageKey(), correct);

                CoordDTO userCoord = new CoordDTO();
                userCoord.setLon(it.getUserLon());
                userCoord.setLat(it.getUserLat());

                var answer = new RecordDetailResponse.UserAnswerInfo(userCoord, it.getSingleScore());
                qDetails.add(new RecordDetailResponse.QuestionDetail(base, answer));
            }
        }

        var gameBase = new RecordDetailResponse.GameRecordBase(
            record.getId(),
            user.getId(),
            user.getUsername(),
            record.getTotalQuestionNum(),
            record.getCreatedAt() != null ? record.getCreatedAt().format(ISO) : null,
            record.getGameType());
        var pc = new RecordDetailResponse.PointChange(
                record.getEarnPoints(), record.getPointBefore(), record.getPointAfter());

        return new RecordDetailResponse(gameBase, qDetails, pc);
    }

    // 简单基于距离的评分：max 50 分，线性衰减到 0 分，半径 10000m
    private int calculateScore(Double correctLon, Double correctLat, double userLon, double userLat) {
        if (correctLon == null || correctLat == null) return 0;
        double meters = haversine(correctLat, correctLon, userLat, userLon);
        double max = 50.0;
        double radius = 10000.0; // 10 km
        double score = Math.max(0.0, max * (1 - meters / radius));
        return (int) Math.round(score);
    }

    // 计算两点之间的距离（米）
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
