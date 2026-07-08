package com.campusguess.social.dto;

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
}