package com.campusguess.demo.model.dto.comment;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long commentId;
    private String content;
    private Long userId;
    private String username;
    private LocalDateTime createTime;
    private Integer likeCount;
    // 自分が「いいね」したかどうかのフラグなどを入れる場合もあります
}