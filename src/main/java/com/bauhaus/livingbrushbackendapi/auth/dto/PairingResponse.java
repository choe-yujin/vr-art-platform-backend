package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 페어링 응답 DTO
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "계정 페어링 응답")
public class PairingResponse {

    @Schema(description = "페어링 코드", example = "ABCD1234")
    private String pairingCode;

    @Schema(description = "전체 페어링 코드 (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
    private String fullPairingCode;

    @Schema(description = "QR 코드 이미지 URL", example = "https://s3.amazonaws.com/qr/pairing-123.png")
    private String qrImageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "만료 시간", example = "2025-01-18T15:30:00")
    private LocalDateTime expiresAt;

    @Schema(description = "만료까지 남은 시간(초)", example = "300")
    private long expiresInSeconds;

    @Builder
    private PairingResponse(String pairingCode, String fullPairingCode, String qrImageUrl, 
                           LocalDateTime expiresAt, long expiresInSeconds) {
        this.pairingCode = pairingCode;
        this.fullPairingCode = fullPairingCode;
        this.qrImageUrl = qrImageUrl;
        this.expiresAt = expiresAt;
        this.expiresInSeconds = expiresInSeconds;
    }

    /**
     * AccountPairing 엔티티로부터 응답 DTO를 생성합니다.
     */
    public static PairingResponse from(com.bauhaus.livingbrushbackendapi.auth.entity.AccountPairing pairing) {
        long expiresInSeconds = java.time.Duration.between(
            java.time.LocalDateTime.now(), 
            pairing.getExpiresAt()
        ).getSeconds();
        
        return PairingResponse.builder()
                .pairingCode(pairing.getShortPairingCode())
                .fullPairingCode(pairing.getPairingCodeString())
                .qrImageUrl(pairing.getQrImageUrl())
                .expiresAt(pairing.getExpiresAt())
                .expiresInSeconds(Math.max(0, expiresInSeconds))
                .build();
    }
}
