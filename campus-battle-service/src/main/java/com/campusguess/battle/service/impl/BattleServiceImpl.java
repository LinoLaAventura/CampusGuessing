package com.campusguess.battle.service.impl;

import com.campusguess.battle.dto.BattleAnswerRequest;
import com.campusguess.battle.dto.BattleInviteRequest;
import com.campusguess.battle.dto.BattleInviteResponse;
import com.campusguess.battle.dto.BattleStateMessage;
import com.campusguess.battle.entity.BattleRoom;
import com.campusguess.battle.entity.BattleRoundRecord;
import com.campusguess.battle.entity.Question;
import com.campusguess.battle.repository.BattleRoomRepository;
import com.campusguess.battle.repository.QuestionRepository;
import com.campusguess.battle.service.BattleRoomCacheService;
import com.campusguess.battle.service.BattleService;
import com.campusguess.battle.service.OnlineUserService;
import com.campusguess.battle.util.DistanceUtil;
import com.campusguess.common.entity.User;
import com.campusguess.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对战核心服务实现
 * 从 demo 迁移，适配微服务架构（Redis 缓存 + 分布式锁）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BattleServiceImpl implements BattleService {

    private final BattleRoomRepository battleRoomRepository;
    private final QuestionRepository questionRepository;
    private final BattleRoomCacheService cacheService;
    private final OnlineUserService onlineUserService;
    private final ObjectMapper objectMapper;

    @Override
    public BattleStateMessage sendInvite(BattleInviteRequest request) {
        String inviter = request.getInviter();
        String invitee = request.getInvitee();

        // 检查邀请者是否在线
        if (!onlineUserService.isOnline(inviter)) {
            throw new BusinessException(400, "邀请者不在线");
        }

        // 检查被邀请者是否在线
        if (!onlineUserService.isOnline(invitee)) {
            throw new BusinessException(400, "被邀请者不在线");
        }

        // 检查邀请者是否已在房间中
        String inviterRoom = onlineUserService.getUserRoom(inviter);
        if (inviterRoom != null) {
            throw new BusinessException(400, "您已在其他房间中");
        }

        // 检查被邀请者是否已在房间中
        String inviteeRoom = onlineUserService.getUserRoom(invitee);
        if (inviteeRoom != null) {
            throw new BusinessException(400, "对方已在其他房间中");
        }

        // 创建房间
        String roomCode = UUID.randomUUID().toString().substring(0, 8);
        BattleRoom room = new BattleRoom();
        room.setRoomCode(roomCode);
        room.setPlayerA(inviter);
        room.setPlayerB(invitee);
        room.setPlayerAHealth(100);
        room.setPlayerBHealth(100);
        room.setStatus(BattleRoom.BattleStatus.WAITING);
        room.setCurrentRound(1);
        room.setGameType(request.getGameType() != null ? request.getGameType() : "好友对战");

        battleRoomRepository.save(room);
        cacheService.cacheRoom(room);

        onlineUserService.setUserRoom(inviter, roomCode);
        onlineUserService.setUserRoom(invitee, roomCode);

        log.info("创建对战房间: {}, 邀请者={}, 被邀请者={}", roomCode, inviter, invitee);

        return BattleStateMessage.builder()
                .roomCode(roomCode)
                .playerA(inviter)
                .playerB(invitee)
                .status(BattleStateMessage.BattleStatus.WAITING)
                .message("等待对方接受邀请...")
                .build();
    }

    @Override
    public BattleStateMessage handleInviteResponse(BattleInviteResponse response) {
        String roomCode = response.getRoomCode();
        BattleRoom room = getRoomForUpdate(roomCode);

        if (room.getStatus() != BattleRoom.BattleStatus.WAITING) {
            throw new BusinessException(400, "房间状态异常");
        }

        if (!Boolean.TRUE.equals(response.getAccepted())) {
            // 拒绝邀请
            room.setStatus(BattleRoom.BattleStatus.FINISHED);
            room.setFinishedAt(LocalDateTime.now());
            battleRoomRepository.save(room);
            cacheService.removeRoom(roomCode);

            onlineUserService.clearUserRoom(room.getPlayerA());
            onlineUserService.clearUserRoom(room.getPlayerB());

            log.info("邀请被拒绝: 房间={}", roomCode);
            return BattleStateMessage.builder()
                    .roomCode(roomCode)
                    .status(BattleStateMessage.BattleStatus.REJECTED)
                    .message("对方拒绝了邀请")
                    .build();
        }

        // 接受邀请，开始对战
        Question firstQuestion = getRandomQuestion();
        room.setCurrentQuestionId(firstQuestion.getId());
        room.setStatus(BattleRoom.BattleStatus.PLAYING);
        room.setStartedAt(LocalDateTime.now());
        room.setPlayerAAnswered(false);
        room.setPlayerBAnswered(false);

        battleRoomRepository.save(room);
        cacheService.cacheRoom(room);

        log.info("对战开始: 房间={}, 第一题ID={}", roomCode, firstQuestion.getId());

        return BattleStateMessage.builder()
                .roomCode(roomCode)
                .playerA(room.getPlayerA())
                .playerB(room.getPlayerB())
                .status(BattleStateMessage.BattleStatus.PLAYING)
                .currentRound(1)
                .questionId(firstQuestion.getId())
                .questionImageUrl(firstQuestion.getImageKey())
                .playerAHealth(room.getPlayerAHealth())
                .playerBHealth(room.getPlayerBHealth())
                .message("对战开始！")
                .build();
    }

    @Override
    public BattleStateMessage submitAnswer(BattleAnswerRequest request) {
        String roomCode = request.getRoomCode();
        String username = request.getUsername();

        // 获取分布式锁
        if (!cacheService.tryLock(roomCode)) {
            throw new BusinessException(429, "系统繁忙，请稍后重试");
        }

        try {
            BattleRoom room = getRoomForUpdate(roomCode);

            if (room.getStatus() != BattleRoom.BattleStatus.PLAYING) {
                throw new BusinessException(400, "游戏未在进行中");
            }

            // 保存答案
            String answerStr = request.getLon() + "," + request.getLat();
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

            // 同步到 Redis 缓存
            cacheService.saveAnswer(roomCode, username, answerStr);

            // 检查双方是否都作答完毕
            if (!room.getPlayerAAnswered() || !room.getPlayerBAnswered()) {
                battleRoomRepository.save(room);
                cacheService.cacheRoom(room);
                return BattleStateMessage.builder()
                        .roomCode(roomCode)
                        .status(BattleStateMessage.BattleStatus.WAITING_ANSWER)
                        .message("等待对方作答...")
                        .build();
            }

            // 双方都作答完毕，计算结果
            BattleStateMessage.RoundResult roundResult = calculateRoundResult(room);
            battleRoomRepository.save(room);
            cacheService.cacheRoom(room);

            // 检查游戏是否结束
            if (room.getStatus() == BattleRoom.BattleStatus.FINISHED) {
                return BattleStateMessage.builder()
                        .roomCode(roomCode)
                        .playerA(room.getPlayerA())
                        .playerB(room.getPlayerB())
                        .status(BattleStateMessage.BattleStatus.FINISHED)
                        .winner(room.getWinner())
                        .playerAHealth(room.getPlayerAHealth())
                        .playerBHealth(room.getPlayerBHealth())
                        .roundResult(roundResult)
                        .message("游戏结束！获胜者: " + room.getWinner())
                        .build();
            }

            return BattleStateMessage.builder()
                    .roomCode(roomCode)
                    .playerA(room.getPlayerA())
                    .playerB(room.getPlayerB())
                    .status(BattleStateMessage.BattleStatus.PLAYING)
                    .currentRound(room.getCurrentRound())
                    .playerAHealth(room.getPlayerAHealth())
                    .playerBHealth(room.getPlayerBHealth())
                    .roundResult(roundResult)
                    .message("回合结束，准备下一轮")
                    .build();
        } finally {
            cacheService.unlock(roomCode);
        }
    }

    @Transactional
    public BattleRoom createRoom(String playerA, String playerB, String gameType) {
        String roomCode = UUID.randomUUID().toString().substring(0, 8);
        BattleRoom room = new BattleRoom();
        room.setRoomCode(roomCode);
        room.setPlayerA(playerA);
        room.setPlayerB(playerB);
        room.setPlayerAHealth(100);
        room.setPlayerBHealth(100);
        room.setStatus(BattleRoom.BattleStatus.WAITING);
        room.setCurrentRound(1);
        room.setGameType(gameType != null ? gameType : "好友对战");

        battleRoomRepository.save(room);
        cacheService.cacheRoom(room);

        onlineUserService.setUserRoom(playerA, roomCode);
        onlineUserService.setUserRoom(playerB, roomCode);

        return room;
    }

    public BattleRoom getRoomByCode(String roomCode) {
        // 优先从缓存获取
        return cacheService.getRoom(roomCode)
                .orElseGet(() -> battleRoomRepository.findByRoomCode(roomCode)
                        .orElseThrow(() -> new BusinessException(404, "房间不存在")));
    }

    private BattleRoom getRoomForUpdate(String roomCode) {
        return battleRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new BusinessException(404, "房间不存在"));
    }

    public BattleStateMessage.RoundResult calculateRoundResult(BattleRoom room) {
        if (!room.getPlayerAAnswered() || !room.getPlayerBAnswered()) {
            throw new BusinessException(400, "双方尚未全部作答");
        }

        Question question = questionRepository.findById(room.getCurrentQuestionId())
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));

        try {
            String[] answerAParts = room.getPlayerAAnswer().split(",");
            String[] answerBParts = room.getPlayerBAnswer().split(",");

            double lonA = Double.parseDouble(answerAParts[0]);
            double latA = Double.parseDouble(answerAParts[1]);
            double lonB = Double.parseDouble(answerBParts[0]);
            double latB = Double.parseDouble(answerBParts[1]);

            double distanceA = DistanceUtil.calculateDistance(
                    question.getLat(), question.getLon(), latA, lonA);
            double distanceB = DistanceUtil.calculateDistance(
                    question.getLat(), question.getLon(), latB, lonB);

            String damagedPlayer;
            int damage;

            if (distanceA > distanceB) {
                damagedPlayer = room.getPlayerA();
                damage = DistanceUtil.calculateDamage(distanceA);
                room.setPlayerAHealth(Math.max(0, room.getPlayerAHealth() - damage));
            } else if (distanceB > distanceA) {
                damagedPlayer = room.getPlayerB();
                damage = DistanceUtil.calculateDamage(distanceB);
                room.setPlayerBHealth(Math.max(0, room.getPlayerBHealth() - damage));
            } else {
                damagedPlayer = null;
                damage = 0;
            }

            BattleRoundRecord roundRecord = new BattleRoundRecord();
            roundRecord.setRoundNumber(room.getCurrentRound());
            roundRecord.setQuestionId(room.getCurrentQuestionId());
            roundRecord.setPlayerALon(lonA);
            roundRecord.setPlayerALat(latA);
            roundRecord.setPlayerBLon(lonB);
            roundRecord.setPlayerBLat(latB);
            roundRecord.setPlayerADistance(distanceA);
            roundRecord.setPlayerBDistance(distanceB);
            roundRecord.setDamagedPlayer(damagedPlayer);
            roundRecord.setDamage(damage);
            roundRecord.setPlayerAHealthAfter(room.getPlayerAHealth());
            roundRecord.setPlayerBHealthAfter(room.getPlayerBHealth());

            saveRoundRecordToJson(room, roundRecord);

            if (room.getPlayerAHealth() <= 0) {
                room.setStatus(BattleRoom.BattleStatus.FINISHED);
                room.setWinner(room.getPlayerB());
                room.setFinishedAt(LocalDateTime.now());
                battleRoomRepository.save(room);
                saveBattleToRecords(room);
            } else if (room.getPlayerBHealth() <= 0) {
                room.setStatus(BattleRoom.BattleStatus.FINISHED);
                room.setWinner(room.getPlayerA());
                room.setFinishedAt(LocalDateTime.now());
                battleRoomRepository.save(room);
                saveBattleToRecords(room);
            } else {
                battleRoomRepository.save(room);
            }

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

    @Transactional
    public BattleRoom startNewRound(String roomCode) {
        BattleRoom room = getRoomForUpdate(roomCode);

        if (room.getStatus() != BattleRoom.BattleStatus.PLAYING) {
            throw new BusinessException(400, "游戏未在进行中");
        }

        room.setPlayerAAnswered(false);
        room.setPlayerBAnswered(false);
        room.setPlayerAAnswer(null);
        room.setPlayerBAnswer(null);
        room.setCurrentRound(room.getCurrentRound() + 1);

        Question newQuestion = getRandomQuestion();
        room.setCurrentQuestionId(newQuestion.getId());

        battleRoomRepository.save(room);
        cacheService.cacheRoom(room);

        return room;
    }

    public boolean isGameOver(BattleRoom room) {
        return room.getStatus() == BattleRoom.BattleStatus.FINISHED
                || room.getPlayerAHealth() <= 0
                || room.getPlayerBHealth() <= 0;
    }

    @Transactional
    public void saveBattleRecords(BattleRoom room) {
        saveBattleToRecords(room);
    }

    @Transactional
    public void finishBattleAndSaveRecords(String roomCode, String winner) {
        log.info("结束对战并保存记录: 房间={}, 获胜者={}", roomCode, winner);

        BattleRoom room = getRoomForUpdate(roomCode);
        room.setStatus(BattleRoom.BattleStatus.FINISHED);
        room.setWinner(winner);
        room.setFinishedAt(LocalDateTime.now());

        battleRoomRepository.save(room);
        cacheService.cacheRoom(room);

        saveBattleToRecords(room);
    }

    // ==================== 私有辅助方法 ====================

    private Question getRandomQuestion() {
        return questionRepository.findRandomQuestion()
                .orElseThrow(() -> new BusinessException(400, "题库为空，无法开始对战"));
    }

    private void saveRoundRecordToJson(BattleRoom room, BattleRoundRecord roundRecord) {
        try {
            List<BattleRoundRecord> records = getRoundRecordsFromJson(room);
            records.add(roundRecord);
            String json = objectMapper.writeValueAsString(records);
            room.setRoundHistoryJson(json);
            log.debug("回合记录已追加: 房间={}, 当前回合数={}", room.getRoomCode(), records.size());
        } catch (Exception e) {
            log.error("保存回合记录到JSON失败: {}", e.getMessage(), e);
        }
    }

    private List<BattleRoundRecord> getRoundRecordsFromJson(BattleRoom room) {
        try {
            if (room.getRoundHistoryJson() == null || room.getRoundHistoryJson().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(room.getRoundHistoryJson(),
                    new TypeReference<List<BattleRoundRecord>>() {});
        } catch (Exception e) {
            log.error("从JSON读取回合记录失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void saveBattleToRecords(BattleRoom room) {
        try {
            log.info("对战记录已保存: 房间={}, 玩家A={}, 玩家B={}",
                    room.getRoomCode(), room.getPlayerA(), room.getPlayerB());
            // TODO: 当前版本将记录保存到本地 JSON 字段 roundHistoryJson 中
            // 后续可扩展为调用 record-service 的 Feign 接口进行远程持久化
        } catch (Exception e) {
            log.error("保存对战记录失败: {}", e.getMessage(), e);
        }
    }
}