package com.campusguess.record.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "record_items")
@Data
public class RecordItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private Record record;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "user_lon")
    private Double userLon;

    @Column(name = "user_lat")
    private Double userLat;

    @Column(name = "single_score")
    private Integer singleScore;
}