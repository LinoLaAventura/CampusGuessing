package com.campusguess.record.dto;

import com.campusguess.record.entity.RecordItem;
import lombok.Data;

@Data
public class RecordItemResponse {
    private Long id;
    private Long questionId;
    private String imageKey;
    private Double userLon;
    private Double userLat;
    private Double correctLon;
    private Double correctLat;
    private Integer singleScore;

    public static RecordItemResponse fromEntity(RecordItem item) {
        RecordItemResponse r = new RecordItemResponse();
        r.setId(item.getId());
        r.setUserLon(item.getUserLon());
        r.setUserLat(item.getUserLat());
        r.setSingleScore(item.getSingleScore());
        if (item.getQuestion() != null) {
            r.setQuestionId(item.getQuestion().getId());
            r.setImageKey(item.getQuestion().getImageKey());
            r.setCorrectLon(item.getQuestion().getCorrectLon());
            r.setCorrectLat(item.getQuestion().getCorrectLat());
        }
        return r;
    }
}