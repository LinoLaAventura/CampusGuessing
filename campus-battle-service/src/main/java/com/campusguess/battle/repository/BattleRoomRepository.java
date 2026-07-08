package com.campusguess.battle.repository;

import com.campusguess.battle.entity.BattleRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BattleRoomRepository extends JpaRepository<BattleRoom, Long> {

    /**
     * 根据房间代码查找房间
     */
    Optional<BattleRoom> findByRoomCode(String roomCode);

    /**
     * 查找玩家参与的进行中的房间
     */
    Optional<BattleRoom> findByStatusAndPlayerAOrPlayerB(
            BattleRoom.BattleStatus status, String playerA, String playerB);
}