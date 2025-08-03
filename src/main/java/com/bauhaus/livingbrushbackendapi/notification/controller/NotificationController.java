package com.bauhaus.livingbrushbackendapi.notification.controller;

import com.bauhaus.livingbrushbackendapi.notification.dto.NotificationDTO;
import com.bauhaus.livingbrushbackendapi.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 미읽음 알림 목록 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @RequestParam Long userId
    ) {
        log.info("미읽음 알림 조회 요청: userId={}", userId);

        try {
            List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);
            log.info("미읽음 알림 조회 완료: userId={}, count={}", userId, notifications.size());

            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("미읽음 알림 조회 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Long notificationId
    ) {
        log.info("알림 읽음 처리 요청: notificationId={}", notificationId);

        try {
            notificationService.markNotificationAsRead(notificationId);
            log.info("알림 읽음 처리 완료: notificationId={}", notificationId);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패: notificationId={}", notificationId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 테스트용 팔로우 알림 전송
     */
    @PostMapping("/test/follow")
    public ResponseEntity<Void> sendTestFollowNotification(
            @RequestParam Long targetUserId,
            @RequestParam Long followerUserId,
            @RequestParam String followerNickname
    ) {
        log.info("테스트 팔로우 알림 전송: targetUserId={}, followerUserId={}, followerNickname={}",
                targetUserId, followerUserId, followerNickname);

        try {
            notificationService.sendFollowNotification(targetUserId, followerUserId, followerNickname);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("테스트 팔로우 알림 전송 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
