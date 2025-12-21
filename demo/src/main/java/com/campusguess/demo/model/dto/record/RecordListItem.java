package com.campusguess.demo.model.dto.record;

import lombok.Data;

@Data
public class RecordListItem {
    private Long recordId;
    private Integer earnPoints;
    private String gameType;
    private String createdAt; // ISO

    public RecordListItem(Long recordId, Integer earnPoints, String gameType, String createdAt) {
        this.recordId = recordId;
        this.earnPoints = earnPoints;
        this.gameType = gameType;
        this.createdAt = createdAt;
    }
}
