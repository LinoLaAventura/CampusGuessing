package com.campusguess.record.dto;

import lombok.Data;

@Data
public class QuestionRecordRequest {
    private Long questionId;
    private CoordDTO userCoord;
}