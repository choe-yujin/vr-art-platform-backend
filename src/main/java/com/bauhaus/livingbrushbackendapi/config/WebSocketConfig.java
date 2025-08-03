package com.bauhaus.livingbrushbackendapi.config;

import com.bauhaus.livingbrushbackendapi.notification.handler.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final NotificationWebSocketHandler notificationHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/notifications")
                .setAllowedOrigins("*") // 개발용, 프로덕션에서는 도메인 지정
                .withSockJS(); // SockJS 지원으로 호환성 향상
    }
}
