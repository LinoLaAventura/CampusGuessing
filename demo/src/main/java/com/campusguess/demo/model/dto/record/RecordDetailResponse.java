package com.campusguess.demo.model.dto.record;

import lombok.Data;
import java.util.List;

/**
 * 答题记录详情响应
 */
@Data
public class RecordDetailResponse {
    private GameRecordBase gameRecordBase;
    private List<QuestionDetail> questionDetails;
    private PointChange pointChange;

    public RecordDetailResponse(GameRecordBase gameRecordBase, List<QuestionDetail> questionDetails, PointChange pointChange) {
        this.gameRecordBase = gameRecordBase;
        this.questionDetails = questionDetails;
        this.pointChange = pointChange;
    }

    /** 游戏记录基本信息 */
    @Data
    public static class GameRecordBase {
        private Long recordId;
        private Long userId;
        private String username;
        private Integer totalQuestionNum;
        private String createdAt; // ISO string for record creation time
        private String gameType;

        public GameRecordBase(Long recordId, Long userId, String username, Integer totalQuestionNum, String createdAt, String gameType) {
            this.recordId = recordId;
            this.userId = userId;
            this.username = username;
            this.totalQuestionNum = totalQuestionNum;
            this.createdAt = createdAt;
            this.gameType = gameType;
        }
    }

    /** 积分变化信息 */
    @Data
    public static class PointChange {
        private Integer earnPoints;
        private Integer pointBefore;
        private Integer pointAfter;

        public PointChange(Integer earnPoints, Integer pointBefore, Integer pointAfter) {
            this.earnPoints = earnPoints;
            this.pointBefore = pointBefore;
            this.pointAfter = pointAfter;
        }
    }

    /** 单题详情 */
    @Data
    public static class QuestionDetail {
        private QuestionBase questionBase;
        private UserAnswerInfo userAnswerInfo;

        public QuestionDetail(QuestionBase questionBase, UserAnswerInfo userAnswerInfo) {
            this.questionBase = questionBase;
            this.userAnswerInfo = userAnswerInfo;
        }
    }

    /** 题目基本信息 */
    @Data
    public static class QuestionBase {
        private Long questionId;
        private String campus;
        private String difficulty;
        private String imageUrl;
        private CoordDTO correctCoord;

        public QuestionBase(Long questionId, String campus, String difficulty, String imageUrl, CoordDTO correctCoord) {
            this.questionId = questionId;
            this.campus = campus;
            this.difficulty = difficulty;
            this.imageUrl = imageUrl;
            this.correctCoord = correctCoord;
        }
    }

    /** 用户作答信息 */
    @Data
    public static class UserAnswerInfo {
        private CoordDTO userCoord;
        private Integer singleScore;

        public UserAnswerInfo(CoordDTO userCoord, Integer singleScore) {
            this.userCoord = userCoord;
            this.singleScore = singleScore;
        }
    }
}
