package com.campusguess.record.repository;

import com.campusguess.record.entity.RecordItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordItemRepository extends JpaRepository<RecordItem, Long> {
}