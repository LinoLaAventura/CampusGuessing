package com.campusguess.demo.controller;

import com.campusguess.demo.model.dto.battle.*;
import com.campusguess.demo.model.dto.question.QuestionResponse;
import com.campusguess.demo.model.entity.BattleRoom;
import com.campusguess.demo.model.entity.Question;
import com.campusguess.demo.repository.BattleRoomRepository;
import com.campusguess.demo.repository.QuestionRepository;
import com.campusguess.demo.service.BattleService;
import com.campusguess.demo.service.BattleRoomCacheService;
import com.campusguess.demo.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

/**
 * WebSocket对战控制器
 * 处理实时对战消息
 * <p>
 * Redis 集成：房间状态通过 BattleRoomCacheService 缓存到 Redis，
 * 在线状态通过 OnlineUserService（Redis 实现）管理。
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class BattleWebSocketController {

    private final BattleService battleService;
    private final QuestionRepository questionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineUserService onlineUserService;
    private final BattleRoomRepository battleRoomRepository;
    private final BattleRoomCacheService roomCacheService;
    private final SimpUserRegistry simpUserRegistry;

    // ==================== 邀请流程 ====================

    /**
     * 发送对战邀请
     */
    @MessageMapping("/battle/invite")
    public void sendInvite(@Payload BattleInviteRequest request) {
        log.info("收到对战邀请：{} -> {}", request.getFromUsername(), request.getToUsername());

        try {
            // 1. 检查是否自我邀请
            if (request.getFromUsername().equals(request.getToUsername())) {
                BattleStateMessage errorMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .message("不能邀请自己对战")
                        .build();
                sendToUser(request.getFromUsername(), "/queue/battle/state", errorMsg);
                log.warn("用户 {} 尝试邀请自己对战", request.getFromUsername());
                return;
            }

            // 2. 检查发送者是否在对战中
            if (onlineUserService.isUserInBattle(request.getFromUsername())) {
                BattleStateMessage errorMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .message("你正在对战中，无法发起新邀请")
                        .build();
                sendToUser(request.getFromUsername(), "/queue/battle/state", errorMsg);
                return;
            }

            // 3. 检查接收者是否在线
            if (!onlineUserService.isUserOnline(request.getToUsername())) {
                BattleStateMessage errorMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .message("用户 " + request.getToUsername() + " 未在线")
                        .build();
                sendToUser(request.getFromUsername(), "/queue/battle/state", errorMsg);
                return;
            }

            // 4. 检查接收者是否在对战中
            if (onlineUserService.isUserInBattle(request.getToUsername())) {
                BattleStateMessage errorMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .message("用户 " + request.getToUsername() + " 正在对战中")
                        .build();
                sendToUser(request.getFromUsername(), "/queue/battle/state", errorMsg);
                return;
            }

            // 5. 创建房间
            BattleRoom room = battleService.createInvite(
                    request.getFromUsername(), request.getToUsername());

            // 6. 设置游戏模式并持久化
            if (request.getGameType() != null && !request.getGameType().isEmpty()) {
                room.setGameType(request.getGameType());
            }
            battleRoomRepository.save(room);

            // 7. 缓存房间状态到 Redis
            roomCacheService.cacheRoomState(room);

            // 8. 构建并发送邀请消息
            BattleStateMessage message = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.INVITE)
                    .roomCode(room.getRoomCode())
                    .playerA(room.getPlayerA())
                    .playerB(room.getPlayerB())
                    .gameType(room.getGameType())
                    .message(request.getFromUsername() + " 邀请你进行对战")
                    .build();

            sendToUser(request.getToUsername(), "/queue/battle/invite", message);
            log.info("邀请已发送，房间代码：{}", room.getRoomCode());

        } catch (Exception e) {
            log.error("发送邀请失败", e);
        }
}
/**
     * 接受/拒绝邀请
     */
    @MessageMapping("/battle/respond")
    public void respondToInvite(@Payload BattleInviteResponse response) {
        log.info("收到邀请响应：房间={}, 接受={}", response.getRoomCode(), response.getAccepted());

        try {
            if (response.getAccepted()) {
                BattleRoom room = battleService.acceptInvite(
                        response.getRoomCode(), response.getUsername());
                log.info("邀请已接受: playerA={}, playerB={}", room.getPlayerA(), room.getPlayerB());

                onlineUserService.enterBattle(room.getPlayerA(), room.getRoomCode());
                onlineUserService.enterBattle(room.getPlayerB(), room.getRoomCode());

                roomCacheService.cacheRoomState(room);

                Question question = null;
                QuestionResponse questionResp = null;
                if (room.getCurrentQuestionId() != null) {
                    question = questionRepository.findByIdWithAuthor(room.getCurrentQuestionId()).orElse(null);
                    if (question != null) {
                        questionResp = convertToQuestionResponse(question);
                    }
                }

                BattleStateMessage message = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.GAME_START)
                        .roomCode(room.getRoomCode())
                        .playerA(room.getPlayerA())
                        .playerB(room.getPlayerB())
                        .playerAHealth(room.getPlayerAHealth())
                        .playerBHealth(room.getPlayerBHealth())
                        .currentRound(room.getCurrentRound())
                        .question(questionResp)
                        .gameType(room.getGameType())
                        .message("对战开始！")
                        .build();

                sendToUser(room.getPlayerA(), "/queue/battle/state", message);
                sendToUser(room.getPlayerB(), "/queue/battle/state", message);
                log.info("游戏开始: {} vs {}", room.getPlayerA(), room.getPlayerB());

            } else {
                BattleRoom room = battleService.getRoomByCode(response.getRoomCode());
                battleService.rejectInvite(response.getRoomCode(), response.getUsername());

                BattleStateMessage message = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .roomCode(response.getRoomCode())
                        .message(response.getUsername() + " 拒绝了你的邀请")
                        .build();

                sendToUser(room.getPlayerA(), "/queue/battle/state", message);
            }

        } catch (Exception e) {
            log.error("处理邀请响应失败", e);
        }
    }
/**
     * 退出对战
     */
    @MessageMapping("/battle/quit")
    public void quitBattle(@Payload BattleInviteRequest request) {
        String username = request.getFromUsername();
        log.info("用户请求退出对战: {}", username);

        try {
            String roomCode = onlineUserService.getUserRoomCode(username);
            if (roomCode == null) {
                log.warn("用户 {} 不在任何对战中", username);
                return;
            }

            BattleRoom room = battleService.getRoomByCode(roomCode);
            String opponentUsername = room.getPlayerA().equals(username)
                    ? room.getPlayerB() : room.getPlayerA();

            onlineUserService.leaveBattle(room.getPlayerA());
            onlineUserService.leaveBattle(room.getPlayerB());

            battleService.finishBattleAndSaveRecords(roomCode, opponentUsername);

            roomCacheService.evictRoom(roomCode, room.getPlayerA(), room.getPlayerB());

            BattleStateMessage quitMsg = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.GAME_OVER)
                    .roomCode(roomCode)
                    .message("你已退出对战")
                    .winner(opponentUsername)
                    .build();
            sendToUser(username, "/queue/battle/state", quitMsg);

            BattleStateMessage opponentMsg = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.GAME_OVER)
                    .roomCode(roomCode)
                    .message(username + " 退出了对战，你获胜了！")
                    .winner(opponentUsername)
                    .build();
            sendToUser(opponentUsername, "/queue/battle/state", opponentMsg);

            log.info("对战已结束: 房间={}, 退出者={}, 获胜者={}", roomCode, username, opponentUsername);

        } catch (Exception e) {
            log.error("退出对战失败", e);
        }
    }

    /**
     * 提交答案
     */
    @MessageMapping("/battle/answer")
    public void submitAnswer(@Payload BattleAnswerRequest request) {
        log.info("收到答案：房间={}, 用户={}", request.getRoomCode(), request.getUsername());

        // 获取分布式锁，防止并发提交导致重复结算
        if (!roomCacheService.tryLockRoom(request.getRoomCode())) {
            log.warn("房间被锁定，忽略重复提交: roomCode={}, user={}", request.getRoomCode(), request.getUsername());
            return;
        }

        try {
            BattleRoom room = battleService.submitAnswer(
                    request.getRoomCode(), request.getUsername(),
                    request.getLongitude(), request.getLatitude());

            // 缓存玩家答案到 Redis（回合级临时数据，TTL 60s）
            String answerJson = String.format("{\"longitude\":%s,\"latitude\":%s}",
                    request.getLongitude(), request.getLatitude());
            roomCacheService.cachePlayerAnswer(request.getRoomCode(), request.getUsername(), answerJson);

            String otherPlayer = room.getPlayerA().equals(request.getUsername())
                    ? room.getPlayerB() : room.getPlayerA();

            BattleStateMessage answerNotice = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.PLAYER_ANSWERED)
                    .roomCode(room.getRoomCode())
                    .playerAAnswered(room.getPlayerAAnswered())
                    .playerBAnswered(room.getPlayerBAnswered())
                    .message(request.getUsername() + " 已作答")
                    .countdown(30)
                    .build();

            sendToUser(otherPlayer, "/queue/battle/state", answerNotice);

            roomCacheService.cacheRoomState(room);

            if (room.getPlayerAAnswered() && room.getPlayerBAnswered()) {
                handleRoundResult(room);
            } else {
                // 仅一方作答，释放锁让另一方提交
                roomCacheService.unlockRoom(request.getRoomCode());
            }

        } catch (Exception e) {
            log.error("处理答案失败", e);
            roomCacheService.unlockRoom(request.getRoomCode());
        }
    }
/**
     * 处理回合结果
     */
    private void handleRoundResult(BattleRoom room) {
        try {
            BattleStateMessage.RoundResult result = battleService.calculateRoundResult(room);
            final BattleRoom updatedRoom = battleService.getRoomByCode(room.getRoomCode());

            if (battleService.isGameOver(updatedRoom)) {
                BattleStateMessage gameOverMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.GAME_OVER)
                        .roomCode(updatedRoom.getRoomCode())
                        .playerA(updatedRoom.getPlayerA())
                        .playerB(updatedRoom.getPlayerB())
                        .playerAHealth(updatedRoom.getPlayerAHealth())
                        .playerBHealth(updatedRoom.getPlayerBHealth())
                        .winner(updatedRoom.getWinner())
                        .gameType(updatedRoom.getGameType())
                        .roundResult(result)
                        .message("游戏结束！获胜者：" + updatedRoom.getWinner())
                        .build();

                sendToUser(updatedRoom.getPlayerA(), "/queue/battle/state", gameOverMsg);
                sendToUser(updatedRoom.getPlayerB(), "/queue/battle/state", gameOverMsg);

                onlineUserService.leaveBattle(updatedRoom.getPlayerA());
                onlineUserService.leaveBattle(updatedRoom.getPlayerB());

                roomCacheService.evictRoom(updatedRoom.getRoomCode(),
                        updatedRoom.getPlayerA(), updatedRoom.getPlayerB());
                roomCacheService.unlockRoom(updatedRoom.getRoomCode());

            } else {
                BattleStateMessage roundResultMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.ROUND_RESULT)
                        .roomCode(updatedRoom.getRoomCode())
                        .playerAHealth(updatedRoom.getPlayerAHealth())
                        .playerBHealth(updatedRoom.getPlayerBHealth())
                        .currentRound(updatedRoom.getCurrentRound())
                        .gameType(updatedRoom.getGameType())
                        .roundResult(result)
                        .message("第 " + updatedRoom.getCurrentRound() + " 回合结束")
                        .build();

                sendToUser(updatedRoom.getPlayerA(), "/queue/battle/state", roundResultMsg);
                sendToUser(updatedRoom.getPlayerB(), "/queue/battle/state", roundResultMsg);

                roomCacheService.cacheRoomState(updatedRoom);
                roomCacheService.unlockRoom(updatedRoom.getRoomCode());

                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        startNextRound(updatedRoom.getRoomCode());
                    } catch (InterruptedException e) {
                        log.error("延迟失败", e);
                    }
                }).start();
            }

        } catch (Exception e) {
            log.error("处理回合结果失败", e);
        }
    }
/**
     * 开始新回合
     */
    private void startNextRound(String roomCode) {
        try {
            BattleRoom room = battleService.startNewRound(roomCode);

            // 清除上一回合的玩家答案缓存
            roomCacheService.clearPlayerAnswers(roomCode, room.getPlayerA(), room.getPlayerB());

            roomCacheService.cacheRoomState(room);

            Question question = questionRepository.findByIdWithAuthor(room.getCurrentQuestionId())
                    .orElseThrow();
            QuestionResponse questionResp = convertToQuestionResponse(question);

            BattleStateMessage newRoundMsg = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.NEW_QUESTION)
                    .roomCode(room.getRoomCode())
                    .currentRound(room.getCurrentRound())
                    .question(questionResp)
                    .playerAHealth(room.getPlayerAHealth())
                    .playerBHealth(room.getPlayerBHealth())
                    .playerAAnswered(false)
                    .playerBAnswered(false)
                    .message("第 " + room.getCurrentRound() + " 回合开始")
                    .build();

            sendToUser(room.getPlayerA(), "/queue/battle/state", newRoundMsg);
            sendToUser(room.getPlayerB(), "/queue/battle/state", newRoundMsg);

        } catch (Exception e) {
            log.error("开始新回合失败", e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 转换 Question 实体为 QuestionResponse DTO
     */
    private QuestionResponse convertToQuestionResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setTitle(question.getTitle());
        response.setContent(question.getContent());
        response.setImageKey(question.getImageKey());
        response.setCreatedAt(question.getCreatedAt());
        response.setCampus(question.getCampus());
        response.setDifficulty(question.getDifficulty());

        try {
            if (question.getAuthor() != null && org.hibernate.Hibernate.isInitialized(question.getAuthor())) {
                response.setAuthorId(question.getAuthor().getId());
                response.setAuthorUsername(question.getAuthor().getUsername());
            }
        } catch (Exception e) {
            log.warn("无法加载题目作者信息: {}", e.getMessage());
        }

        if (question.getCorrectLon() != null && question.getCorrectLat() != null) {
            QuestionResponse.CorrectCoord coord = new QuestionResponse.CorrectCoord();
            coord.setLon(question.getCorrectLon());
            coord.setLat(question.getCorrectLat());
            response.setCorrectCoord(coord);
        }

        return response;
    }

    /**
     * 安全发送消息给指定用户
     */
    private void sendToUser(String username, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
        } catch (Exception e) {
            log.error("发送消息失败: user={}, dest={}", username, destination, e);
        }
    }

    /**
     * 获取在线用户数（调试用）
     */
    @MessageMapping("/battle/onlineCount")
    public void getOnlineUserCount() {
        try {
            int count = simpUserRegistry.getUserCount();
            java.util.Set<String> usernames = simpUserRegistry.getUsers().stream()
                    .map(SimpUser::getName)
                    .collect(java.util.stream.Collectors.toSet());
            log.info("在线用户数: {}, 用户: {}", count, usernames);
        } catch (Exception e) {
            log.error("获取在线用户数失败", e);
        }
    }
}