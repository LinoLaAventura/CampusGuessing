package com.campusguess.record.service;

import com.campusguess.record.dto.RecordRequest;
import com.campusguess.record.dto.RecordResponse;

import java.util.List;

public interface RecordService {
    RecordResponse submitRecord(RecordRequest request);
    List<RecordResponse> getUserRecords(Long userId);
    RecordResponse getRecordDetail(Long userId, Long recordId);
}