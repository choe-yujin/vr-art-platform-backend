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
        registry.addHandler(notificationHandler, "/notifications/websocket")
                .setAllowedOrigins(
                        "http://api.livingbrush.shop:8888",
                        "https://api.livingbrush.shop",
                        "http://localhost:3000", // 개발용
                        "http://localhost:8080"  // 개발용
                );
    }
}
