package com.campusguess.record.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecordCreateResponse {
    private Long recordId;
    private Integer earnPoints;
}