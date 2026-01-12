package com.campusguess.demo.repository;

import com.campusguess.demo.model.entity.BattleRoundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BattleRoundRecordRepository extends JpaRepository<BattleRoundRecord, Long> {
    
    /**
     * 根据对战房间ID查询所有回合记录
     */
    List<BattleRoundRecord> findByBattleRoomIdOrderByRoundNumberAsc(Long battleRoomId);
}
