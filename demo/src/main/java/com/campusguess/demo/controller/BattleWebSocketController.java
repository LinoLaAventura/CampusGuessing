package com.campusguess.demo.controller;

import com.campusguess.demo.model.dto.battle.*;
import com.campusguess.demo.model.dto.question.QuestionResponse;
import com.campusguess.demo.model.entity.BattleRoom;
import com.campusguess.demo.model.entity.Question;
import com.campusguess.demo.repository.BattleRoomRepository;
import com.campusguess.demo.repository.QuestionRepository;
import com.campusguess.demo.service.BattleService;
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
    private final SimpUserRegistry simpUserRegistry;

    /**
     * 发送对战邀请
     * 客户端发送到：/app/battle/invite
     */
    @MessageMapping("/battle/invite")
    public void sendInvite(@Payload BattleInviteRequest request) {
        log.info("收到对战邀请：{} -> {}", request.getFromUsername(), request.getToUsername());

        try {
            // 检查是否自我邀请
            if (request.getFromUsername().equals(request.getToUsername())) {
                BattleStateMessage errorMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .message("❌ 不能邀请自己对战")
                        .build();
                messagingTemplate.convertAndSendToUser(
                        request.getFromUsername(),
                        "/queue/battle/state",
                        errorMsg
                );
                log.warn("用户 {} 尝试邀请自己对战", request.getFromUsername());
                return;
            }
            
            // 检查发送者是否在对战中
            if (onlineUserService.isUserInBattle(request.getFromUsername())) {
                BattleStateMessage errorMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .message("❌ 你正在对战中，无法发起新邀请")
                        .build();
                messagingTemplate.convertAndSendToUser(
                        request.getFromUsername(),
                        "/queue/battle/state",
                        errorMsg
                );
                return;
            }
            
            // 检查接收者是否在线
            if (!onlineUserService.isUserOnline(request.getToUsername())) {
                BattleStateMessage errorMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .message("❌ 用户 " + request.getToUsername() + " 未在线")
                        .build();
                messagingTemplate.convertAndSendToUser(
                        request.getFromUsername(),
                        "/queue/battle/state",
                        errorMsg
                );
                return;
            }
            
            // 检查接收者是否在对战中
            if (onlineUserService.isUserInBattle(request.getToUsername())) {
                BattleStateMessage errorMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .message("❌ 用户 " + request.getToUsername() + " 正在对战中")
                        .build();
                messagingTemplate.convertAndSendToUser(
                        request.getFromUsername(),
                        "/queue/battle/state",
                        errorMsg
                );
                return;
            }

            // 创建房间
            BattleRoom room = battleService.createInvite(
                    request.getFromUsername(),
                    request.getToUsername()
            );

            // 构建邀请消息
            BattleStateMessage message = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.INVITE)
                    .roomCode(room.getRoomCode())
                    .playerA(room.getPlayerA())
                    .playerB(room.getPlayerB())
                    .message(request.getFromUsername() + " 邀请你进行对战")
                    .build();

            // 发送邀请通知给被邀请者
            messagingTemplate.convertAndSendToUser(
                    request.getToUsername(),
                    "/queue/battle/invite",
                    message
            );

            log.info("邀请已发送，房间代码：{}", room.getRoomCode());

        } catch (Exception e) {
            log.error("发送邀请失败", e);
        }
    }

    /**
     * 接受/拒绝邀请
     * 客户端发送到：/app/battle/respond
     */
    @MessageMapping("/battle/respond")
    public void respondToInvite(@Payload BattleInviteResponse response) {
        log.info("收到邀请响应：房间={}, 接受={}, 用户={}",
                response.getRoomCode(), response.getAccepted(), response.getUsername());

        try {
            if (response.getAccepted()) {
                log.info("开始处理接受邀请...");
                
                // 接受邀请
                BattleRoom room = battleService.acceptInvite(
                        response.getRoomCode(),
                        response.getUsername()
                );
                log.info("邀请已接受，房间信息: playerA={}, playerB={}, questionId={}", 
                        room.getPlayerA(), room.getPlayerB(), room.getCurrentQuestionId());
                
                // 标记双方进入对战
                onlineUserService.enterBattle(room.getPlayerA(), room.getRoomCode());
                onlineUserService.enterBattle(room.getPlayerB(), room.getRoomCode());
                log.info("双方已标记进入对战状态");

                // 获取题目信息
                Question question = null;
                QuestionResponse questionResp = null;
                if (room.getCurrentQuestionId() != null) {
                    question = questionRepository.findByIdWithAuthor(room.getCurrentQuestionId()).orElse(null);
                    if (question != null) {
                        questionResp = convertToQuestionResponse(question);
                        log.info("题目已获取: id={}, title={}", question.getId(), question.getTitle());
                    } else {
                        log.warn("题目不存在: id={}", room.getCurrentQuestionId());
                    }
                } else {
                    log.warn("房间没有设置题目ID");
                }

                // 构建游戏开始消息
                BattleStateMessage message = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.GAME_START)
                        .roomCode(room.getRoomCode())
                        .playerA(room.getPlayerA())
                        .playerB(room.getPlayerB())
                        .playerAHealth(room.getPlayerAHealth())
                        .playerBHealth(room.getPlayerBHealth())
                        .currentRound(room.getCurrentRound())
                        .question(questionResp)
                        .message("对战开始！")
                        .build();
                log.info("GAME_START消息已构建");

                // 通知双方玩家
                log.info("准备发送GAME_START消息给玩家A: {}", room.getPlayerA());
                try {
                    sendToUser(room.getPlayerA(), "/queue/battle/state", message);
                    log.info("GAME_START消息已发送给玩家A: {}", room.getPlayerA());
                } catch (Exception e) {
                    log.error("发送给玩家A失败: {}", e.getMessage(), e);
                }
                
                log.info("准备发送GAME_START消息给玩家B: {}", room.getPlayerB());
                try {
                    sendToUser(room.getPlayerB(), "/queue/battle/state", message);
                    log.info("GAME_START消息已发送给玩家B: {}", room.getPlayerB());
                } catch (Exception e) {
                    log.error("发送给玩家B失败: {}", e.getMessage(), e);
                }
                
                log.info("游戏开始通知处理完成: {} vs {}", room.getPlayerA(), room.getPlayerB());

            } else {
                // 拒绝邀请
                BattleRoom room = battleService.getRoomByCode(response.getRoomCode());
                battleService.rejectInvite(response.getRoomCode(), response.getUsername());

                // 通知邀请者
                BattleStateMessage message = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.INVITE_REJECTED)
                        .roomCode(response.getRoomCode())
                        .message(response.getUsername() + " 拒绝了你的邀请")
                        .build();

                messagingTemplate.convertAndSendToUser(
                        room.getPlayerA(),
                        "/queue/battle/state",
                        message
                );
            }

        } catch (Exception e) {
            log.error("处理邀请响应失败", e);
        }
    }
    
    /**
     * 退出对战
     * 客户端发送到：/app/battle/quit
     */
    @MessageMapping("/battle/quit")
    public void quitBattle(@Payload BattleInviteRequest request) {
        String username = request.getFromUsername();
        log.info("用户请求退出对战: {}", username);
        
        try {
            // 获取用户所在房间
            String roomCode = onlineUserService.getUserRoomCode(username);
            if (roomCode == null) {
                log.warn("用户 {} 不在任何对战中", username);
                return;
            }
            
            BattleRoom room = battleService.getRoomByCode(roomCode);
            String opponentUsername = room.getPlayerA().equals(username) 
                    ? room.getPlayerB() 
                    : room.getPlayerA();
            
            // 清除双方对战状态
            onlineUserService.leaveBattle(room.getPlayerA());
            onlineUserService.leaveBattle(room.getPlayerB());
            
            // 设置房间状态为结束，退出者判负
            room.setStatus(BattleRoom.BattleStatus.FINISHED);
            room.setWinner(opponentUsername);
            room.setFinishedAt(java.time.LocalDateTime.now());
            battleRoomRepository.save(room);
            
            // 通知退出者
            BattleStateMessage quitMsg = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.GAME_OVER)
                    .roomCode(roomCode)
                    .message("你已退出对战")
                    .winner(opponentUsername)
                    .build();
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/battle/state",
                    quitMsg
            );
            
            // 通知对手
            BattleStateMessage opponentMsg = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.GAME_OVER)
                    .roomCode(roomCode)
                    .message(username + " 退出了对战，你获胜了！")
                    .winner(opponentUsername)
                    .build();
            messagingTemplate.convertAndSendToUser(
                    opponentUsername,
                    "/queue/battle/state",
                    opponentMsg
            );
            
            log.info("对战已结束: 房间={}, 退出者={}, 获胜者={}", roomCode, username, opponentUsername);
            
        } catch (Exception e) {
            log.error("退出对战失败", e);
        }
    }

    /**
     * 提交答案
     * 客户端发送到：/app/battle/answer
     */
    @MessageMapping("/battle/answer")
    public void submitAnswer(@Payload BattleAnswerRequest request) {
        log.info("收到答案：房间={}, 用户={}", request.getRoomCode(), request.getUsername());

        try {
            // 保存答案
            BattleRoom room = battleService.submitAnswer(
                    request.getRoomCode(),
                    request.getUsername(),
                    request.getLongitude(),
                    request.getLatitude()
            );

            // 通知对方玩家已作答
            String otherPlayer = room.getPlayerA().equals(request.getUsername())
                    ? room.getPlayerB()
                    : room.getPlayerA();

            BattleStateMessage answerNotice = BattleStateMessage.builder()
                    .type(BattleStateMessage.MessageType.PLAYER_ANSWERED)
                    .roomCode(room.getRoomCode())
                    .playerAAnswered(room.getPlayerAAnswered())
                    .playerBAnswered(room.getPlayerBAnswered())
                    .message(request.getUsername() + " 已作答")
                    .countdown(30) // 30秒倒计时
                    .build();

            messagingTemplate.convertAndSendToUser(
                    otherPlayer,
                    "/queue/battle/state",
                    answerNotice
            );

            // 检查是否双方都已作答
            if (room.getPlayerAAnswered() && room.getPlayerBAnswered()) {
                // 计算回合结果
                handleRoundResult(room);
            }

        } catch (Exception e) {
            log.error("处理答案失败", e);
        }
    }

    /**
     * 处理回合结果
     */
    private void handleRoundResult(BattleRoom room) {
        try {
            // 计算结果
            BattleStateMessage.RoundResult result = battleService.calculateRoundResult(room);

            // 刷新房间状态
            final BattleRoom updatedRoom = battleService.getRoomByCode(room.getRoomCode());

            // 检查游戏是否结束
            if (battleService.isGameOver(updatedRoom)) {
                // 游戏结束
                BattleStateMessage gameOverMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.GAME_OVER)
                        .roomCode(updatedRoom.getRoomCode())
                        .playerA(updatedRoom.getPlayerA())
                        .playerB(updatedRoom.getPlayerB())
                        .playerAHealth(updatedRoom.getPlayerAHealth())
                        .playerBHealth(updatedRoom.getPlayerBHealth())
                        .winner(updatedRoom.getWinner())
                        .roundResult(result)
                        .message("游戏结束！获胜者：" + updatedRoom.getWinner())
                        .build();

                messagingTemplate.convertAndSendToUser(
                        updatedRoom.getPlayerA(),
                        "/queue/battle/state",
                        gameOverMsg
                );
                messagingTemplate.convertAndSendToUser(
                        updatedRoom.getPlayerB(),
                        "/queue/battle/state",
                        gameOverMsg
                );
                
                // 对战结束，清除对战状态
                onlineUserService.leaveBattle(updatedRoom.getPlayerA());
                onlineUserService.leaveBattle(updatedRoom.getPlayerB());

            } else {
                // 发送回合结果
                BattleStateMessage roundResultMsg = BattleStateMessage.builder()
                        .type(BattleStateMessage.MessageType.ROUND_RESULT)
                        .roomCode(updatedRoom.getRoomCode())
                        .playerAHealth(updatedRoom.getPlayerAHealth())
                        .playerBHealth(updatedRoom.getPlayerBHealth())
                        .currentRound(updatedRoom.getCurrentRound())
                        .roundResult(result)
                        .message("第 " + updatedRoom.getCurrentRound() + " 回合结束")
                        .build();

                messagingTemplate.convertAndSendToUser(
                        updatedRoom.getPlayerA(),
                        "/queue/battle/state",
                        roundResultMsg
                );
                messagingTemplate.convertAndSendToUser(
                        updatedRoom.getPlayerB(),
                        "/queue/battle/state",
                        roundResultMsg
                );

                // 延迟3秒后开始新回合
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

            // 获取新题目（使用fetch查询避免懒加载问题）
            Question question = questionRepository.findByIdWithAuthor(room.getCurrentQuestionId())
                    .orElseThrow();
            QuestionResponse questionResp = convertToQuestionResponse(question);

            // 通知双方新回合开始
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

            messagingTemplate.convertAndSendToUser(
                    room.getPlayerA(),
                    "/queue/battle/state",
                    newRoundMsg
            );
            messagingTemplate.convertAndSendToUser(
                    room.getPlayerB(),
                    "/queue/battle/state",
                    newRoundMsg
            );

        } catch (Exception e) {
            log.error("开始新回合失败", e);
        }
    }

    /**
     * 转换Question为QuestionResponse
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
        
        // 设置作者信息（处理懒加载问题）
        try {
            if (question.getAuthor() != null && org.hibernate.Hibernate.isInitialized(question.getAuthor())) {
                response.setAuthorId(question.getAuthor().getId());
                response.setAuthorUsername(question.getAuthor().getUsername());
            }
        } catch (Exception e) {
            log.warn("无法加载题目作者信息: {}", e.getMessage());
        }
        
        // 设置正确坐标
        if (question.getCorrectLon() != null && question.getCorrectLat() != null) {
            QuestionResponse.CorrectCoord coord = new QuestionResponse.CorrectCoord();
            coord.setLon(question.getCorrectLon());
            coord.setLat(question.getCorrectLat());
            response.setCorrectCoord(coord);
        }
        
        return response;
    }
    
    /**
     * 发送消息给用户（带诊断日志）
     */
    private void sendToUser(String username, String destination, Object message) {
        // 检查用户是否在SimpUserRegistry中注册
        SimpUser simpUser = simpUserRegistry.getUser(username);
        if (simpUser == null) {
            log.warn("⚠️ 用户 {} 未在SimpUserRegistry中注册! 已注册用户: {}", 
                    username, 
                    simpUserRegistry.getUsers().stream().map(SimpUser::getName).toList());
        } else {
            log.info("✅ 用户 {} 已注册，会话数: {}", username, simpUser.getSessions().size());
        }
        
        messagingTemplate.convertAndSendToUser(username, destination, message);
        log.info("消息已发送给用户 {}: destination={}", username, destination);
    }
}
