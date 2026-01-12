package com.campusguess.demo.config;

import com.campusguess.demo.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
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

/**
 * WebSocket配置类
 * 用于双人对战功能的实时通信
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final OnlineUserService onlineUserService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理，用于向客户端广播消息
        // /topic 用于广播消息（一对多）
        // /queue 用于点对点消息（一对一）
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 客户端发送消息的前缀
        registry.setApplicationDestinationPrefixes("/app");
        
        // 点对点消息的前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 添加通道拦截器，用于设置用户身份
        registration.interceptors(new UserChannelInterceptor());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端通过此端点连接WebSocket
        registry.addEndpoint("/ws-battle")
                .setAllowedOriginPatterns("*") // 允许所有来源（开发环境）
                .addInterceptors(new UserHandshakeInterceptor()) // 添加用户身份拦截器
                .setHandshakeHandler(new CustomHandshakeHandler()) // 自定义握手处理器设置Principal
                .withSockJS(); // 启用SockJS降级选项
    }
    
    /**
     * 自定义握手处理器
     * 在握手时设置用户Principal，这样convertAndSendToUser才能正确工作
     */
    private class CustomHandshakeHandler extends org.springframework.web.socket.server.support.DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String username = (String) attributes.get("username");
            if (username != null) {
                log.info("CustomHandshakeHandler: 设置Principal username={}", username);
                return () -> username;
            }
            log.warn("CustomHandshakeHandler: 无法设置Principal，username为空");
            return null;
        }
    }

    /**
     * WebSocket握手拦截器
     * 从URL参数中提取username并设置为WebSocket会话的用户身份
     */
    private class UserHandshakeInterceptor implements HandshakeInterceptor {
        
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                      WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                // 从URL参数中获取username
                String username = servletRequest.getServletRequest().getParameter("username");
                if (username != null && !username.isEmpty()) {
                    attributes.put("username", username);
                    log.info("WebSocket握手: 提取到username={}", username);
                } else {
                    log.warn("WebSocket握手: 未找到username参数");
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Exception exception) {
            // 握手后处理（可选）
        }
    }

    /**
     * 通道拦截器
     * 在STOMP CONNECT命令时设置用户身份
     */
    private class UserChannelInterceptor implements ChannelInterceptor {
        
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            
            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                // 从WebSocket会话属性中获取username
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                String username = sessionAttributes != null ? (String) sessionAttributes.get("username") : null;
                
                if (username != null) {
                    // 创建一个简单的Principal对象
                    Principal principal = () -> username;
                    accessor.setUser(principal);
                    log.info("WebSocket CONNECT: 设置用户身份 username={}", username);
                } else {
                    log.warn("WebSocket CONNECT: 无法获取username，sessionAttributes={}", sessionAttributes);
                }
            }
            
            return message;
        }
    }
    
    /**
     * WebSocket连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;
        String sessionId = accessor.getSessionId();
        log.info("WebSocket连接事件: username={}, sessionId={}", username, sessionId);
        if (username != null && sessionId != null) {
            onlineUserService.userOnline(username, sessionId);
        }
    }
    
    /**
     * WebSocket断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;
        log.info("WebSocket断开事件: username={}, sessionId={}", username, accessor.getSessionId());
        if (username != null) {
            onlineUserService.userOffline(username);
        }
    }
}
