package com.campusguess.demo.config;

import com.campusguess.demo.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final OnlineUserService onlineUserService;

    @Value("${broker.type:simple}")
    private String brokerType;

    @Value("${broker.relay-host:localhost}")
    private String relayHost;

    @Value("${broker.relay-port:61613}")
    private int relayPort;

    @Value("${broker.relay-login:guest}")
    private String relayLogin;

    @Value("${broker.relay-passcode:guest}")
    private String relayPasscode;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if ("external".equalsIgnoreCase(brokerType)) {
            log.info("WebSocket STOMP Broker: {}:{}", relayHost, relayPort);
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(relayLogin)
                    .setClientPasscode(relayPasscode)
                    .setSystemLogin(relayLogin)
                    .setSystemPasscode(relayPasscode);
        } else {
            log.info("WebSocket SimpleBroker");
            registry.enableSimpleBroker("/topic", "/queue");
        }
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
@Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new UserChannelInterceptor());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-battle")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new UserHandshakeInterceptor())
                .withSockJS();
    }

    private class UserHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                      WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                String username = servletRequest.getServletRequest().getParameter("username");
                if (username != null && !username.isEmpty()) {
                    attributes.put("username", username);
                    log.info("WebSocket握手: username={}", username);
                } else {
                    log.warn("WebSocket握手: 未找到username参数");
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Exception exception) {
        }
    }

    private class UserChannelInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                String username = sessionAttributes != null ? (String) sessionAttributes.get("username") : null;
                if (username != null) {
                    Principal principal = () -> username;
                    accessor.setUser(principal);
                    log.info("WebSocket CONNECT: username={}", username);
                } else {
                    log.warn("WebSocket CONNECT: 无法获取username");
                }
            }
            return message;
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;
        String sessionId = accessor.getSessionId();
        log.info("WebSocket连接: username={}, sessionId={}", username, sessionId);
        if (username != null && sessionId != null) {
            onlineUserService.userOnline(username, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;
        log.info("WebSocket断开: username={}, sessionId={}", username, accessor.getSessionId());
        if (username != null) {
            onlineUserService.userOffline(username);
        }
    }
}