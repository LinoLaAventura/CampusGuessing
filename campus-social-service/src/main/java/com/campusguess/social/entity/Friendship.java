package com.campusguess.social.entity;

import com.campusguess.common.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(columnNames = { "sender_id", "receiver_id" }))
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "handled_type", length = 20)
    private String handledType;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }

    public enum FriendshipStatus {
        PENDING("待处理"),
        APPROVED("已接受"),
        REJECTED("已拒绝");

        private final String description;

        FriendshipStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}