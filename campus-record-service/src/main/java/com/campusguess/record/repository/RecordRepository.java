package com.campusguess.record.repository;

import com.campusguess.record.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    List<Record> findByUserIdOrderByCreatedAtDesc(Long userId);
}