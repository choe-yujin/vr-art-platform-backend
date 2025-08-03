package com.bauhaus.livingbrushbackendapi.notification.service;

import com.bauhaus.livingbrushbackendapi.notification.dto.NotificationDTO;
import com.bauhaus.livingbrushbackendapi.notification.entity.Notification;
import com.bauhaus.livingbrushbackendapi.notification.handler.NotificationWebSocketHandler;
import com.bauhaus.livingbrushbackendapi.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 서비스 - V1 스키마 100% 호환
 * 
 * 주요 기능:
 * 1. 팔로우 알림 전송 ("정아님이 방금 팔로우했습니다")
 * 2. WebSocket 실시간 전송 + Redis 큐잉
 * 3. PostgreSQL 영구 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final int NOTIFICATION_QUEUE_EXPIRE_HOURS = 24;
    
    /**
     * 팔로우 알림 전송
     * @param targetUserId 알림을 받을 사용자 ID
     * @param followerUserId 팔로우한 사용자 ID
     * @param followerNickname 팔로우한 사용자 닉네임
     */
    public void sendFollowNotification(Long targetUserId, Long followerUserId, String followerNickname) {
        try {
            // 1. PostgreSQL에 알림 영구 저장
            Notification notification = saveNotificationToDB(targetUserId, followerUserId, followerNickname);
            
            // 2. 알림 DTO 생성
            NotificationDTO notificationDTO = createNotificationDTO(notification, followerNickname);
            String notificationJson = objectMapper.writeValueAsString(notificationDTO);
            
            // 3. 실시간 전송 시도
            boolean sentRealtime = NotificationWebSocketHandler.sendNotificationToUser(targetUserId, notificationJson);
            
            if (!sentRealtime) {
                // 4. WebSocket 미연결 시 Redis 큐에 저장
                queueNotificationInRedis(targetUserId, notificationJson);
            }
            
            log.info("팔로우 알림 전송 완료: {} -> {}", followerNickname, targetUserId);
            
        } catch (Exception e) {
            log.error("팔로우 알림 전송 실패", e);
        }
    }
    
    /**
     * 알림을 DB에 영구 저장 (V1 스키마 100% 호환)
     */
    private Notification saveNotificationToDB(Long targetUserId, Long followerUserId, String followerNickname) {
        Notification notification = Notification.builder()
            .userId(targetUserId)
            .notificationType("new_follower")
            .title("새 팔로워")
            .message(followerNickname + "님이 방금 팔로우했습니다")
            .relatedId(followerUserId)
            .isRead(false)
            .isEnabled(true) // 실제 전송된 알림이므로 enabled = true
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
            
        return notificationRepository.save(notification);
    }
    
    /**
     * 알림 DTO 생성
     */
    private NotificationDTO createNotificationDTO(Notification notification, String followerNickname) {
        return NotificationDTO.builder()
            .notificationId(notification.getNotificationId())
            .type(notification.getNotificationType())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .relatedId(notification.getRelatedId())
            .relatedUserNickname(followerNickname)
            .createdAt(notification.getCreatedAt())
            .isRead(notification.getIsRead())
            .build();
    }
    
    /**
     * WebSocket 미연결 시 Redis 큐에 알림 저장
     */
    private void queueNotificationInRedis(Long userId, String notificationJson) {
        String queueKey = "notifications:queue:" + userId;
        
        // Redis List에 알림 추가 (FIFO 순서)
        redisTemplate.opsForList().leftPush(queueKey, notificationJson);
        
        // 24시간 후 자동 만료
        redisTemplate.expire(queueKey, Duration.ofHours(NOTIFICATION_QUEUE_EXPIRE_HOURS));
        
        log.info("사용자 {} 알림 Redis 큐에 저장 완료", userId);
    }
    
    /**
     * 알림 읽음 처리
     */
    public void markNotificationAsRead(Long notificationId) {
        notificationRepository.findById(notificationId)
            .ifPresent(notification -> {
                notification.setIsRead(true);
                notification.setUpdatedAt(LocalDateTime.now());
                notificationRepository.save(notification);
                log.info("알림 {} 읽음 처리 완료", notificationId);
            });
    }
    
    /**
     * 사용자의 미읽음 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
            .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
            
        return notifications.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Notification 엔티티를 DTO로 변환
     */
    private NotificationDTO convertToDTO(Notification notification) {
        return NotificationDTO.builder()
            .notificationId(notification.getNotificationId())
            .type(notification.getNotificationType())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .relatedId(notification.getRelatedId())
            .createdAt(notification.getCreatedAt())
            .isRead(notification.getIsRead())
            .build();
    }
}
