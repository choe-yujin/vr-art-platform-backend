package com.bauhaus.livingbrushbackendapi.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

import java.time.LocalDateTime;

/**
 * QR 코드 엔티티
 * 
 * 작품에 대한 QR 코드 정보를 관리합니다.
 * 하나의 작품은 여러 개의 QR 코드를 가질 수 있으며(이력 관리),
 * is_active 필드로 현재 활성화된 QR 코드를 구분합니다.
 */
@Entity
@Table(name = "qr_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long qrId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @JdbcTypeCode(SqlTypes.UUID) // (핵심) 이 필드를 데이터베이스의 네이티브 UUID 타입에 매핑합니다.
    @Column(name = "qr_token", unique = true, nullable = false, updatable = false)
    private UUID qrToken;

    @Column(name = "qr_image_url", length = 2048)
    private String qrImageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private QrCode(Artwork artwork, UUID qrToken, String qrImageUrl, Boolean isActive) {
        this.artwork = artwork;
        this.qrToken = qrToken; // <-- 이제 UUID를 UUID에 할당하므로 문제 해결
        this.qrImageUrl = qrImageUrl;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * QR 코드를 비활성화합니다.
     * 작품이 비공개로 전환되거나 새로운 QR 코드가 생성될 때 사용됩니다.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * QR 이미지 URL을 업데이트합니다.
     * QR 이미지 생성 및 저장 완료 후 호출됩니다.
     */
    public void updateQrImageUrl(String qrImageUrl) {
        this.qrImageUrl = qrImageUrl;
    }
}
