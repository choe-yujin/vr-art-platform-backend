package com.bauhaus.livingbrushbackendapi.auth.entity;

import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 계정 페어링 엔티티
 * 
 * VR-AR 계정 연동을 위한 임시 페어링 코드를 관리합니다.
 * 보안을 위해 짧은 만료 시간(5분)을 가집니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Entity
@Table(name = "account_pairings", indexes = {
        @Index(name = "account_pairings_pairing_code_idx", columnList = "pairing_code"),
        @Index(name = "account_pairings_expires_at_idx", columnList = "expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@DynamicUpdate
public class AccountPairing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pairing_id")
    private Long pairingId;

    @Column(name = "pairing_code", nullable = false, unique = true)
    private UUID pairingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ar_user_id", nullable = false)
    private User arUser; // AR 앱에서 연동을 요청한 사용자

    @Column(name = "qr_image_url", length = 500)
    private String qrImageUrl;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "linked_meta_user_id", length = 255)
    private String linkedMetaUserId; // VR에서 페어링 완료 시 저장

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private AccountPairing(User arUser, UUID pairingCode, String qrImageUrl, LocalDateTime expiresAt) {
        this.arUser = arUser;
        this.pairingCode = pairingCode;
        this.qrImageUrl = qrImageUrl;
        this.expiresAt = expiresAt;
        this.isUsed = false;
    }

    /**
     * 새로운 페어링 요청을 생성합니다.
     * 
     * @param arUser AR 앱 사용자
     * @param expirationMinutes 만료 시간(분)
     * @return 새로운 AccountPairing 인스턴스
     */
    public static AccountPairing createPairingRequest(User arUser, int expirationMinutes) {
        UUID pairingCode = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
        
        return AccountPairing.builder()
                .arUser(arUser)
                .pairingCode(pairingCode)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * QR 코드 이미지 URL을 설정합니다.
     */
    public void setQrImageUrl(String qrImageUrl) {
        this.qrImageUrl = qrImageUrl;
    }

    /**
     * 페어링이 완료되었는지 확인합니다.
     */
    public boolean isCompleted() {
        return this.isUsed && this.linkedMetaUserId != null && this.completedAt != null;
    }

    /**
     * 페어링이 만료되었는지 확인합니다.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 페어링이 유효한지 확인합니다 (만료되지 않고, 사용되지 않음).
     */
    public boolean isValid() {
        return !isExpired() && !isUsed;
    }

    /**
     * VR에서 페어링을 완료합니다.
     * 
     * @param metaUserId Meta 사용자 ID
     */
    public void completePairing(String metaUserId) {
        if (isExpired()) {
            throw new IllegalStateException("만료된 페어링 코드입니다.");
        }
        if (isUsed) {
            throw new IllegalStateException("이미 사용된 페어링 코드입니다.");
        }
        
        this.linkedMetaUserId = metaUserId;
        this.isUsed = true;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 페어링 코드를 문자열로 반환합니다 (하이픈 포함).
     */
    public String getPairingCodeString() {
        return this.pairingCode.toString();
    }

    /**
     * 사용자 친화적인 페어링 코드를 반환합니다 (첫 8자리만).
     */
    public String getShortPairingCode() {
        return this.pairingCode.toString().substring(0, 8).toUpperCase();
    }
}
