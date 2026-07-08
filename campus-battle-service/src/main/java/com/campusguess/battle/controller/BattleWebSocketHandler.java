package com.campusguess.battle.controller;

import com.campusguess.battle.dto.BattleAnswerRequest;
import com.campusguess.battle.dto.BattleInviteRequest;
import com.campusguess.battle.dto.BattleInviteResponse;
import com.campusguess.battle.dto.BattleStateMessage;
import com.campusguess.battle.entity.BattleRoom;
import com.campusguess.battle.entity.Question;
import com.campusguess.battle.repository.BattleRoomRepository;
import com.campusguess.battle.repository.QuestionRepository;
import com.campusguess.battle.service.BattleRoomCacheService;
import com.campusguess.battle.service.BattleService;
import com.campusguess.battle.service.OnlineUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对战 WebSocket 处理器
 * 基于原始 WebSocket 协议，使用 JSON 消息格式进行通信
 * 消息类型：login, invite, invite_response, answer, next_round, heartbeat
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BattleWebSocketHandler extends TextWebSocketHandler {

    private final BattleService battleService;
    private final QuestionRepository questionRepository;
    private final OnlineUserService onlineUserService;
    private final BattleRoomRepository battleRoomRepository;
    private final BattleRoomCacheService roomCacheService;
    private final ObjectMapper objectMapper;

    /** sessionId -> WebSocketSession */
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    /** username -> sessionId */
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessionMap.put(sessionId, session);
        log.info("WebSocket 连接建立: sessionId={}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        String payload = message.getPayload();
        log.debug("收到消息: sessionId={}, payload={}", sessionId, payload);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.has("type") ? root.get("type").asText() : "";

            switch (type) {
                case "login":
                    handleLogin(session, root);
                    break;
                case "invite":
                    handleInvite(session, root);
                    break;
                case "invite_response":
                    handleInviteResponse(session, root);
                    break;
                case "answer":
                    handleAnswer(session, root);
                    break;
                case "next_round":
                    handleNextRound(session, root);
                    break;
                case "heartbeat":
                    handleHeartbeat(session, root);
                    break;
                default:
                    sendError(session, "未知消息类型: " + type);
            }
        } catch (Exception e) {
            log.error("处理消息失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessionMap.remove(sessionId);

        String username = onlineUserService.getUsernameBySession(sessionId);
        if (username != null) {
            onlineUserService.userOffline(username, sessionId);
            userSessionMap.remove(username);
            log.info("用户下线: {}", username);
        }
        log.info("WebSocket 连接关闭: sessionId={}, status={}", sessionId, status);
    }

    // ==================== 消息处理 ====================

    private void handleLogin(WebSocketSession session, JsonNode root) {
        String username = root.get("username").asText();
        String sessionId = session.getId();

        onlineUserService.userOnline(username, sessionId);
        userSessionMap.put(username, sessionId);

        log.info("用户登录: username={}, sessionId={}", username, sessionId);
        sendToSession(session, createSimpleMessage("login_ok", "登录成功"));
    }

    private void handleInvite(WebSocketSession session, JsonNode root) {
        String inviter = root.get("inviter").asText();
        String invitee = root.get("invitee").asText();
        String gameType = root.has("gameType") ? root.get("gameType").asText() : "好友对战";

        BattleInviteRequest request = BattleInviteRequest.builder()
                .inviter(inviter)
                .invitee(invitee)
                .gameType(gameType)
                .build();

        try {
            BattleStateMessage result = battleService.sendInvite(request);

            // 通知邀请者
            sendToUser(inviter, result);
            // 通知被邀请者
            BattleStateMessage inviteeMsg = BattleStateMessage.builder()
                    .roomCode(result.getRoomCode())
                    .playerA(inviter)
                    .playerB(invitee)
                    .status(BattleStateMessage.BattleStatus.WAITING)
                    .message(inviter + " 邀请你对战")
                    .build();
            sendToUser(invitee, inviteeMsg);
        } catch (Exception e) {
            sendToUser(inviter, createSimpleMessage("error", e.getMessage()));
        }
    }

    private void handleInviteResponse(WebSocketSession session, JsonNode root) {
        String roomCode = root.get("roomCode").asText();
        boolean accepted = root.get("accepted").asBoolean();
        String username = root.get("username").asText();

        BattleInviteResponse response = BattleInviteResponse.builder()
                .roomCode(roomCode)
                .accepted(accepted)
                .username(username)
                .build();

        try {
            BattleStateMessage result = battleService.handleInviteResponse(response);

            if (result.getPlayerA() != null) {
                sendToUser(result.getPlayerA(), result);
            }
            if (result.getPlayerB() != null) {
                sendToUser(result.getPlayerB(), result);
            }
        } catch (Exception e) {
            sendToUser(username, createSimpleMessage("error", e.getMessage()));
        }
    }

    private void handleAnswer(WebSocketSession session, JsonNode root) {
        String roomCode = root.get("roomCode").asText();
        String username = root.get("username").asText();
        double lon = root.get("lon").asDouble();
        double lat = root.get("lat").asDouble();

        BattleAnswerRequest request = BattleAnswerRequest.builder()
                .roomCode(roomCode)
                .username(username)
                .lon(lon)
                .lat(lat)
                .build();

        try {
            BattleStateMessage result = battleService.submitAnswer(request);

            BattleRoom room = battleRoomRepository.findByRoomCode(roomCode).orElse(null);
            if (room != null) {
                sendToUser(room.getPlayerA(), result);
                sendToUser(room.getPlayerB(), result);
            }
        } catch (Exception e) {
            sendToUser(username, createSimpleMessage("error", e.getMessage()));
        }
    }

    private void handleNextRound(WebSocketSession session, JsonNode root) {
        String roomCode = root.get("roomCode").asText();
        String username = root.get("username").asText();

        try {
            BattleRoom room = ((com.campusguess.battle.service.impl.BattleServiceImpl) battleService)
                    .startNewRound(roomCode);

            Question question = questionRepository.findById(room.getCurrentQuestionId())
                    .orElse(null);

            BattleStateMessage msg = BattleStateMessage.builder()
                    .roomCode(roomCode)
                    .playerA(room.getPlayerA())
                    .playerB(room.getPlayerB())
                    .status(BattleStateMessage.BattleStatus.PLAYING)
                    .currentRound(room.getCurrentRound())
                    .questionId(room.getCurrentQuestionId())
                    .questionImageUrl(question != null ? question.getImageKey() : null)
                    .playerAHealth(room.getPlayerAHealth())
                    .playerBHealth(room.getPlayerBHealth())
                    .message("第 " + room.getCurrentRound() + " 回合开始")
                    .build();

            sendToUser(room.getPlayerA(), msg);
            sendToUser(room.getPlayerB(), msg);
        } catch (Exception e) {
            sendToUser(username, createSimpleMessage("error", e.getMessage()));
        }
    }

    private void handleHeartbeat(WebSocketSession session, JsonNode root) {
        String username = root.get("username").asText();
        String sessionId = session.getId();
        onlineUserService.heartbeat(username, sessionId);
        sendToSession(session, createSimpleMessage("heartbeat_ok", "ok"));
    }

    // ==================== 发送工具方法 ====================

    private void sendToUser(String username, Object message) {
        String sessionId = onlineUserService.getSessionId(username);
        if (sessionId != null) {
            WebSocketSession session = sessionMap.get(sessionId);
            if (session != null && session.isOpen()) {
                sendToSession(session, message);
            } else {
                log.warn("用户 {} 的 WebSocket 会话不可用", username);
            }
        }
    }

    private void sendToSession(WebSocketSession session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("发送消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        sendToSession(session, createSimpleMessage("error", errorMessage));
    }

    private Map<String, Object> createSimpleMessage(String type, String message) {
        return Map.of("type", type, "message", message);
    }
}