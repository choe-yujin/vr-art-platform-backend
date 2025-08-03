package com.bauhaus.livingbrushbackendapi.notification.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 엔티티 - V1 스키마 100% 호환
 * 
 * V1 스키마 정의:
 * - notification_id: BIGSERIAL PRIMARY KEY
 * - user_id: BIGINT NOT NULL (FK to users)
 * - notification_type: VARCHAR(50) NOT NULL
 * - title: VARCHAR(255) NOT NULL
 * - message: TEXT NOT NULL
 * - related_id: BIGINT (nullable)
 * - is_read: BOOLEAN NOT NULL DEFAULT FALSE
 * - is_enabled: BOOLEAN NOT NULL DEFAULT FALSE
 * - created_at: TIMESTAMP WITH TIME ZONE DEFAULT NOW()
 * - updated_at: TIMESTAMP WITH TIME ZONE DEFAULT NOW()
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "related_id")
    private Long relatedId;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false; // V1 스키마 기본값과 일치
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
