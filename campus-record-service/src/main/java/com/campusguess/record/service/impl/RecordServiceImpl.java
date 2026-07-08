package com.campusguess.record.service.impl;

import com.campusguess.common.exception.BusinessException;
import com.campusguess.record.dto.QuestionRecordRequest;
import com.campusguess.record.dto.RecordRequest;
import com.campusguess.record.dto.RecordResponse;
import com.campusguess.record.entity.Question;
import com.campusguess.record.entity.Record;
import com.campusguess.record.entity.RecordItem;
import com.campusguess.common.entity.User;
import com.campusguess.record.repository.QuestionRepository;
import com.campusguess.record.repository.RecordItemRepository;
import com.campusguess.record.repository.RecordRepository;
import com.campusguess.record.repository.UserRepository;
import com.campusguess.record.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final RecordItemRepository recordItemRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public RecordResponse submitRecord(RecordRequest request) {
        if (request == null || request.getUserId() == null
                || request.getQuestionRecords() == null || request.getQuestionRecords().isEmpty()) {
            throw new BusinessException(400, "参数错误");
        }

        if (request.getGameType() == null || request.getGameType().isBlank()) {
            throw new BusinessException(400, "游戏类型不能为空");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));

        int totalNum = request.getQuestionRecords().size();

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

        record.setEarnPoints(earnPoints);
        int before = user.getPoints() != null ? user.getPoints() : 0;
        record.setPointBefore(before);
        record.setPointAfter(before + earnPoints);

        Record saved = recordRepository.save(record);

        for (RecordItem it : items) {
            it.setRecord(saved);
        }
        recordItemRepository.saveAll(items);

        // 更新用户积分
        user.setPoints(before + earnPoints);
        userRepository.save(user);

        return RecordResponse.fromEntity(saved);
    }

    @Override
    public List<RecordResponse> getUserRecords(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        List<Record> records = recordRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<RecordResponse> list = new ArrayList<>();
        for (Record r : records) {
            list.add(RecordResponse.fromEntity(r));
        }
        return list;
    }

    @Override
    public RecordResponse getRecordDetail(Long userId, Long recordId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(404, "记录不存在"));

        if (!record.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权限访问该记录");
        }

        return RecordResponse.fromEntity(record);
    }

    /**
     * 基于距离的评分：最高 50 分，线性衰减到 0 分，半径 10000m
     */
    private int calculateScore(Double correctLon, Double correctLat, double userLon, double userLat) {
        if (correctLon == null || correctLat == null) return 0;
        double meters = haversine(correctLat, correctLon, userLat, userLon);
        double max = 50.0;
        double radius = 10000.0;
        double score = Math.max(0.0, max * (1 - meters / radius));
        return (int) Math.round(score);
    }

    /**
     * Haversine 公式计算两点之间的距离（米）
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}