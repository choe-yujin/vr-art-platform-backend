package com.bauhaus.livingbrushbackendapi.notification.handler;

import com.bauhaus.livingbrushbackendapi.notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // 사용자별 WebSocket 세션 관리
    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("=== WebSocket 연결 시도 시작 ===");
        log.info("WebSocket 연결 시도: URI={}", session.getUri());
        log.info("WebSocket 세션 정보: remoteAddress={}, localAddress={}",
                session.getRemoteAddress(), session.getLocalAddress());
        log.info("WebSocket 헤더: {}", session.getHandshakeHeaders());

        try {
            Long userId = getUserIdFromSession(session);

            if (userId != null) {
                userSessions.put(userId, session);
                log.info("사용자 {} WebSocket 연결 성공", userId);

                // Redis에서 대기 중인 알림 즉시 전송
                sendPendingNotifications(userId, session);
            } else {
                log.warn("WebSocket 연결 시 사용자 ID를 찾을 수 없음");
                session.close();
            }
        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 오류 발생", e);
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            log.info("사용자 {} WebSocket 연결 해제 - 상태: {}", userId, status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            JsonNode jsonNode = objectMapper.readTree(payload);

            String type = jsonNode.get("type").asText();
            if ("read_notification".equals(type)) {
                Long notificationId = jsonNode.get("notificationId").asLong();
                markNotificationAsRead(notificationId);
                log.info("알림 읽음 처리: {}", notificationId);
            } else if ("ping".equals(type)) {
                // 연결 유지를 위한 ping 응답
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
        } catch (Exception e) {
            log.error("WebSocket 메시지 처리 실패", e);
        }
    }

    /**
     * 특정 사용자에게 실시간 알림 전송
     */
    public static boolean sendNotificationToUser(Long userId, String notification) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(notification));
                log.info("사용자 {}에게 실시간 알림 전송 완료", userId);
                return true;
            } catch (IOException e) {
                log.error("WebSocket 알림 전송 실패: 사용자 {}", userId, e);
                userSessions.remove(userId); // 끊어진 세션 제거
            }
        } else {
            log.debug("사용자 {} WebSocket 미연결 상태", userId);
        }
        return false;
    }

    /**
     * Redis에서 대기 중인 알림들을 세션 연결 시 즉시 전송
     */
    private void sendPendingNotifications(Long userId, WebSocketSession session) {
        try {
            String queueKey = "notifications:queue:" + userId;
            List<String> pendingNotifications = redisTemplate.opsForList().range(queueKey, 0, -1);

            if (pendingNotifications != null && !pendingNotifications.isEmpty()) {
                for (String notification : pendingNotifications) {
                    session.sendMessage(new TextMessage(notification));
                }

                // 전송 완료된 알림 Redis에서 제거
                redisTemplate.delete(queueKey);
                log.info("사용자 {}의 대기 알림 {} 개 전송 완료", userId, pendingNotifications.size());
            }
        } catch (Exception e) {
            log.error("대기 알림 전송 실패: 사용자 {}", userId, e);
        }
    }

    /**
     * URL 쿼리에서 사용자 ID 추출
     * 예: ws://localhost:8080/notifications?userId=123
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            log.info("WebSocket 연결 시도: URI={}", session.getUri());
            String query = session.getUri().getQuery();
            log.info("WebSocket 쿼리: {}", query);

            if (query != null && query.contains("userId=")) {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                Long userId = Long.parseLong(userIdStr);
                log.info("사용자 ID 추출 성공: {}", userId);
                return userId;
            } else {
                log.warn("userId 파라미터가 없습니다. 쿼리: {}", query);
            }
        } catch (Exception e) {
            log.error("사용자 ID 추출 실패", e);
        }
        return null;
    }

    /**
     * 알림 읽음 처리 (NotificationService와 연동)
     */
    private void markNotificationAsRead(Long notificationId) {
        notificationService.markNotificationAsRead(notificationId);
        log.info("알림 {} 읽음 처리 완료", notificationId);
    }

    /**
     * 현재 연결된 사용자 수 조회 (모니터링용)
     */
    public static int getConnectedUserCount() {
        return userSessions.size();
    }
}
