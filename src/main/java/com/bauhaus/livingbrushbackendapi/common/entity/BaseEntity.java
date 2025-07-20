package com.bauhaus.livingbrushbackendapi.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime; // [수정] LocalDateTime -> ZonedDateTime

/**
 * 모든 엔티티가 상속받는 공통 타임스탬프 필드 베이스 클래스 (JPA Auditing 적용)
 * V1 DB 스크립트(TIMESTAMP WITH TIME ZONE)와 완벽 호환
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class) // [수정] JPA Auditing 리스너를 등록합니다.
public abstract class BaseEntity {

    /**
     * 엔티티 생성 시간
     * V1 DB: created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
     */
    @CreatedDate // [수정] 엔티티 생성 시 자동으로 날짜를 주입합니다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt; // [수정] 시간대 정보를 포함하는 ZonedDateTime 사용

    /**
     * 엔티티 최종 수정 시간
     * V1 DB: updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
     */
    @LastModifiedDate // [수정] 엔티티 수정 시 자동으로 날짜를 주입합니다.
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt; // [수정] 시간대 정보를 포함하는 ZonedDateTime 사용

    /*
     * [제거] @PrePersist, @PreUpdate 메소드는 더 이상 필요 없습니다.
     * AuditingEntityListener가 이 역할을 대신 수행합니다.
     */
}