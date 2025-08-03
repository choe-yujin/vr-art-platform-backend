package com.bauhaus.livingbrushbackendapi.notification.controller;

import com.bauhaus.livingbrushbackendapi.notification.dto.NotificationDTO;
import com.bauhaus.livingbrushbackendapi.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 API 컨트롤러
 * 
 * WebSocket 실시간 알림과 연동하여 알림 목록 조회, 읽음 처리 기능 제공
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification", description = "알림 API - WebSocket 연동")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 사용자의 미읽음 알림 목록 조회
     */
    @GetMapping("/unread")
    @Operation(summary = "미읽음 알림 목록 조회", description = "현재 사용자의 읽지 않은 알림 목록을 최신순으로 조회합니다.")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @Parameter(description = "사용자 ID", example = "1") 
            @RequestHeader("User-Id") Long userId) {
        
        log.info("미읽음 알림 조회: userId={}", userId);
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "알림 ID", example = "1") 
            @PathVariable Long notificationId,
            @Parameter(description = "사용자 ID", example = "1") 
            @RequestHeader("User-Id") Long userId) {
        
        log.info("알림 읽음 처리: notificationId={}, userId={}", notificationId, userId);
        notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * WebSocket 연결 상태 확인 (디버깅용)
     */
    @GetMapping("/websocket/status")
    @Operation(summary = "WebSocket 연결 상태 확인", description = "현재 WebSocket에 연결된 사용자 수를 확인합니다.")
    public ResponseEntity<String> getWebSocketStatus() {
        int connectedUsers = com.bauhaus.livingbrushbackendapi.notification.handler.NotificationWebSocketHandler.getConnectedUserCount();
        return ResponseEntity.ok("WebSocket 연결된 사용자 수: " + connectedUsers);
    }
}
