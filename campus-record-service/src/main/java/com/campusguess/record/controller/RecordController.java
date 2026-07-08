package com.campusguess.record.controller;

import com.campusguess.common.response.ApiResponse;
import com.campusguess.record.dto.RecordRequest;
import com.campusguess.record.dto.RecordResponse;
import com.campusguess.record.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecordResponse>> submitRecord(@Valid @RequestBody RecordRequest request) {
        RecordResponse response = recordService.submitRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("记录提交成功", response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> getUserRecords(@PathVariable Long userId) {
        List<RecordResponse> records = recordService.getUserRecords(userId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", records));
    }

    @GetMapping("/user/{userId}/detail/{recordId}")
    public ResponseEntity<ApiResponse<RecordResponse>> getRecordDetail(
            @PathVariable Long userId, @PathVariable Long recordId) {
        RecordResponse detail = recordService.getRecordDetail(userId, recordId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", detail));
    }
}