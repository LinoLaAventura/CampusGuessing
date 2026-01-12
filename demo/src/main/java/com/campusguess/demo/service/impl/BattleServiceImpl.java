package com.campusguess.demo.service.impl;

import com.campusguess.demo.exception.BusinessException;
import com.campusguess.demo.model.dto.battle.BattleStateMessage;
import com.campusguess.demo.model.entity.BattleRoom;
import com.campusguess.demo.model.entity.Question;
import com.campusguess.demo.repository.BattleRoomRepository;
import com.campusguess.demo.repository.QuestionRepository;
import com.campusguess.demo.service.BattleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 对战服务实现类
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BattleServiceImpl implements BattleService {

    private final BattleRoomRepository battleRoomRepository;
    private final QuestionRepository questionRepository;

    @Override
    public BattleRoom createInvite(String fromUsername, String toUsername) {
        // 生成唯一房间代码
        String roomCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 随机选择一道题目
        Question question = getRandomQuestion();

        BattleRoom room = new BattleRoom();
        room.setRoomCode(roomCode);
        room.setPlayerA(fromUsername);
        room.setPlayerB(toUsername);
        room.setCurrentQuestionId(question.getId());
        room.setStatus(BattleRoom.BattleStatus.WAITING);
        room.setPlayerAHealth(100);
        room.setPlayerBHealth(100);
        room.setCurrentRound(1);

        return battleRoomRepository.save(room);
    }

    @Override
    public BattleRoom acceptInvite(String roomCode, String username) {
        BattleRoom room = getRoomByCode(roomCode);

        if (!room.getPlayerB().equals(username)) {
            throw new BusinessException(403, "您不是该房间的被邀请者");
        }

        if (room.getStatus() != BattleRoom.BattleStatus.WAITING) {
            throw new BusinessException(400, "该房间已不在等待状态");
        }

        room.setStatus(BattleRoom.BattleStatus.PLAYING);
        room.setStartedAt(LocalDateTime.now());

        return battleRoomRepository.save(room);
    }

    @Override
    public void rejectInvite(String roomCode, String username) {
        BattleRoom room = getRoomByCode(roomCode);

        if (!room.getPlayerB().equals(username)) {
            throw new BusinessException(403, "您不是该房间的被邀请者");
        }

        // 直接删除房间或标记为已结束
        battleRoomRepository.delete(room);
    }

    @Override
    public BattleRoom submitAnswer(String roomCode, String username, Double longitude, Double latitude) {
        BattleRoom room = getRoomByCode(roomCode);

        if (room.getStatus() != BattleRoom.BattleStatus.PLAYING) {
            throw new BusinessException(400, "游戏未在进行中");
        }

        // 构建答案字符串 (格式: "lon,lat")
        String answerStr = longitude + "," + latitude;

        // 判断是玩家A还是玩家B
        if (room.getPlayerA().equals(username)) {
            if (room.getPlayerAAnswered()) {
                throw new BusinessException(400, "您已经作答过了");
            }
            room.setPlayerAAnswer(answerStr);
            room.setPlayerAAnswered(true);
        } else if (room.getPlayerB().equals(username)) {
            if (room.getPlayerBAnswered()) {
                throw new BusinessException(400, "您已经作答过了");
            }
            room.setPlayerBAnswer(answerStr);
            room.setPlayerBAnswered(true);
        } else {
            throw new BusinessException(403, "您不在该房间内");
        }

        return battleRoomRepository.save(room);
    }

    @Override
    public BattleRoom getRoomByCode(String roomCode) {
        return battleRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new BusinessException(404, "房间不存在"));
    }

    @Override
    public BattleStateMessage.RoundResult calculateRoundResult(BattleRoom room) {
        if (!room.getPlayerAAnswered() || !room.getPlayerBAnswered()) {
            throw new BusinessException(400, "双方尚未全部作答");
        }

        // 获取题目正确答案
        Question question = questionRepository.findById(room.getCurrentQuestionId())
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));

        try {
            // 解析答案 (格式: "lon,lat")
            String[] answerAParts = room.getPlayerAAnswer().split(",");
            String[] answerBParts = room.getPlayerBAnswer().split(",");
            
            double lonA = Double.parseDouble(answerAParts[0]);
            double latA = Double.parseDouble(answerAParts[1]);
            double lonB = Double.parseDouble(answerBParts[0]);
            double latB = Double.parseDouble(answerBParts[1]);

            // 计算距离（使用Haversine公式）
            double distanceA = calculateDistance(
                    question.getCorrectLat(), question.getCorrectLon(),
                    latA, lonA
            );
            double distanceB = calculateDistance(
                    question.getCorrectLat(), question.getCorrectLon(),
                    latB, lonB
            );

            // 判定扣血
            String damagedPlayer;
            int damage;

            if (distanceA > distanceB) {
                // 玩家A偏离更远，扣血
                damagedPlayer = room.getPlayerA();
                damage = calculateDamage(distanceA);
                room.setPlayerAHealth(Math.max(0, room.getPlayerAHealth() - damage));
            } else if (distanceB > distanceA) {
                // 玩家B偏离更远，扣血
                damagedPlayer = room.getPlayerB();
                damage = calculateDamage(distanceB);
                room.setPlayerBHealth(Math.max(0, room.getPlayerBHealth() - damage));
            } else {
                // 平局，不扣血
                damagedPlayer = null;
                damage = 0;
            }

            // 检查是否有玩家血量归零
            if (room.getPlayerAHealth() <= 0) {
                room.setStatus(BattleRoom.BattleStatus.FINISHED);
                room.setWinner(room.getPlayerB());
                room.setFinishedAt(LocalDateTime.now());
            } else if (room.getPlayerBHealth() <= 0) {
                room.setStatus(BattleRoom.BattleStatus.FINISHED);
                room.setWinner(room.getPlayerA());
                room.setFinishedAt(LocalDateTime.now());
            }

            battleRoomRepository.save(room);

            return BattleStateMessage.RoundResult.builder()
                    .playerADistance(distanceA)
                    .playerBDistance(distanceB)
                    .damagedPlayer(damagedPlayer)
                    .damage(damage)
                    .build();

        } catch (Exception e) {
            log.error("计算回合结果失败", e);
            throw new BusinessException(500, "计算回合结果失败");
        }
    }

    @Override
    public BattleRoom startNewRound(String roomCode) {
        BattleRoom room = getRoomByCode(roomCode);

        if (room.getStatus() != BattleRoom.BattleStatus.PLAYING) {
            throw new BusinessException(400, "游戏未在进行中");
        }

        // 重置作答状态
        room.setPlayerAAnswered(false);
        room.setPlayerBAnswered(false);
        room.setPlayerAAnswer(null);
        room.setPlayerBAnswer(null);

        // 增加回合数
        room.setCurrentRound(room.getCurrentRound() + 1);

        // 随机选择新题目
        Question newQuestion = getRandomQuestion();
        room.setCurrentQuestionId(newQuestion.getId());

        return battleRoomRepository.save(room);
    }

    @Override
    public boolean isGameOver(BattleRoom room) {
        return room.getStatus() == BattleRoom.BattleStatus.FINISHED
                || room.getPlayerAHealth() <= 0
                || room.getPlayerBHealth() <= 0;
    }

    /**
     * 随机获取一道题目
     */
    private Question getRandomQuestion() {
        List<Question> allQuestions = questionRepository.findAll();
        if (allQuestions.isEmpty()) {
            throw new BusinessException(400, "题库为空，无法开始对战");
        }
        Random random = new Random();
        return allQuestions.get(random.nextInt(allQuestions.size()));
    }

    /**
     * 计算两点间距离（Haversine公式，返回米）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 地球半径（米）

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 根据距离计算伤害值（使用对数函数）
     * 伤害随距离先迅速增长，然后缓慢增长
     * 公式: damage = a * log10(distance/b + 1) + c
     * 其中 a 控制增长速度，b 控制曲线陡峭度，c 是基础伤害
     */
    private int calculateDamage(double distance) {
        // 参数调优：
        // a = 25: 控制伤害增长幅度
        // b = 50: 控制曲线陡峭程度（越小曲线越陡）
        // c = 8:  最小伤害（0米时的伤害）
        
        double a = 25.0;
        double b = 50.0;
        double c = 8.0;
        
        // 对数伤害公式
        double damage = a * Math.log10(distance / b + 1) + c;
        
        // 设置伤害上限和下限
        int finalDamage = (int) Math.round(damage);
        finalDamage = Math.max(5, finalDamage);   // 最小伤害5
        finalDamage = Math.min(45, finalDamage);  // 最大伤害45
        
        return finalDamage;
    }
}
