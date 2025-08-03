package com.bauhaus.livingbrushbackendapi.notification.repository;

import com.bauhaus.livingbrushbackendapi.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 사용자의 미읽음 알림 목록 조회 (최신순)
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    
    /**
     * 사용자의 모든 알림 목록 조회 (최신순)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 사용자의 미읽음 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    Long countUnreadNotifications(@Param("userId") Long userId);
}
