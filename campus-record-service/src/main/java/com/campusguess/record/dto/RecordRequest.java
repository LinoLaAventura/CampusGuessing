package com.campusguess.record.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class RecordRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String gameType;

    @NotNull
    private List<QuestionRecordRequest> questionRecords;
}