package com.campusguess.record.dto;

import com.campusguess.record.entity.Record;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class RecordResponse {
    private Long id;
    private Long userId;
    private String username;
    private Integer totalQuestionNum;
    private Integer earnPoints;
    private Integer pointBefore;
    private Integer pointAfter;
    private String gameType;
    private LocalDateTime createdAt;
    private List<RecordItemResponse> items;

    public static RecordResponse fromEntity(Record record) {
        RecordResponse r = new RecordResponse();
        r.setId(record.getId());
        r.setTotalQuestionNum(record.getTotalQuestionNum());
        r.setEarnPoints(record.getEarnPoints());
        r.setPointBefore(record.getPointBefore());
        r.setPointAfter(record.getPointAfter());
        r.setGameType(record.getGameType());
        r.setCreatedAt(record.getCreatedAt());
        if (record.getUser() != null) {
            r.setUserId(record.getUser().getId());
            r.setUsername(record.getUser().getUsername());
        }
        if (record.getItems() != null) {
            r.setItems(record.getItems().stream()
                    .map(RecordItemResponse::fromEntity)
                    .collect(Collectors.toList()));
        }
        return r;
    }
}