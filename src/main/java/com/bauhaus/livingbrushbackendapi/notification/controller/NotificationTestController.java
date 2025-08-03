package com.bauhaus.livingbrushbackendapi.notification.controller;

import com.bauhaus.livingbrushbackendapi.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 테스트 컨트롤러 (개발 전용)
 * 
 * WebSocket 알림 시스템을 테스트하기 위한 API 제공
 */
@RestController
@RequestMapping("/api/test/notifications")
@RequiredArgsConstructor
@Slf4j
@Profile("!prod") // 프로덕션 환경에서는 비활성화
@Tag(name = "Notification Test", description = "알림 테스트 API (개발 전용)")
public class NotificationTestController {

    private final NotificationService notificationService;

    /**
     * 팔로우 알림 테스트 전송
     */
    @PostMapping("/test-follow")
    @Operation(summary = "팔로우 알림 테스트", 
               description = "WebSocket 알림 시스템 테스트를 위한 가짜 팔로우 알림을 전송합니다.")
    public ResponseEntity<String> testFollowNotification(
            @Parameter(description = "알림 받을 사용자 ID", example = "1") 
            @RequestParam Long targetUserId,
            @Parameter(description = "팔로우한 사용자 ID", example = "2") 
            @RequestParam Long followerId,
            @Parameter(description = "팔로우한 사용자 닉네임", example = "정아") 
            @RequestParam(defaultValue = "정아") String followerNickname) {
        
        log.info("팔로우 알림 테스트: targetUserId={}, followerId={}, followerNickname={}", 
                targetUserId, followerId, followerNickname);
        
        notificationService.sendFollowNotification(targetUserId, followerId, followerNickname);
        
        return ResponseEntity.ok("팔로우 알림 테스트 전송 완료: " + followerNickname + "님이 " + targetUserId + "님을 팔로우했습니다");
    }
}
