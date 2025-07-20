package com.bauhaus.livingbrushbackendapi.qrcode.entity;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * QR 코드 엔티티 (V1 DB 스크립트 완벽 호환)
 * 
 * 순수한 데이터 컨테이너 (Anemic Domain Model)
 * 모든 비즈니스 로직은 QrCodeDomainService에서 처리
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Entity
@Table(name = "qr_codes", uniqueConstraints = {
        @UniqueConstraint(name = "qr_codes_qr_token_uk", columnNames = {"qr_token"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@DynamicInsert
@DynamicUpdate
public class QrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_id")
    private Long qrId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false,
                foreignKey = @ForeignKey(name = "qr_codes_artwork_id_fk"))
    private Artwork artwork;

    @Column(name = "qr_token", nullable = false, unique = true, updatable = false,
            columnDefinition = "UUID DEFAULT gen_random_uuid()")
    @JdbcTypeCode(SqlTypes.UUID)
    @Builder.Default
    private UUID qrToken = UUID.randomUUID();

    @Column(name = "qr_image_url", length = 2048)
    private String qrImageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ========== 타임스탬프 (DB 스크립트와 완벽 일치) ==========
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ========== 단순한 상태 확인 메서드만 허용 ==========

    /**
     * 활성화된 QR 코드인지 단순 확인 (boolean 체크만)
     */
    public boolean isActive() {
        return this.isActive != null && this.isActive;
    }

    /**
     * 비활성화된 QR 코드인지 단순 확인 (boolean 체크만)
     */
    public boolean isInactive() {
        return this.isActive == null || !this.isActive;
    }

    /**
     * QR 이미지가 존재하는지 단순 확인 (null/empty 체크만)
     */
    public boolean hasQrImage() {
        return this.qrImageUrl != null && !this.qrImageUrl.trim().isEmpty();
    }

    /**
     * QR 토큰이 존재하는지 단순 확인 (null 체크만)
     */
    public boolean hasValidToken() {
        return this.qrToken != null;
    }

    // ========== JPA 콜백 메서드 ==========
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
