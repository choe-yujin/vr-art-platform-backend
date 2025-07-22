package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 페어링 상태 응답 DTO
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "페어링 상태 응답")
public class PairingStatusResponse {

    @Schema(description = "페어링 상태", example = "COMPLETED", 
            allowableValues = {"PENDING", "COMPLETED", "EXPIRED", "INVALID"})
    private String status;

    @Schema(description = "상태 메시지", example = "페어링이 완료되었습니다.")
    private String message;

    @Schema(description = "페어링 완료 여부", example = "true")
    private boolean isCompleted;

    @Schema(description = "연동된 Meta 사용자 ID", example = "1234567890")
    private String linkedMetaUserId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "페어링 완료 시간", example = "2025-01-18T15:25:30")
    private LocalDateTime completedAt;

    @Builder
    private PairingStatusResponse(String status, String message, boolean isCompleted, 
                                 String linkedMetaUserId, LocalDateTime completedAt) {
        this.status = status;
        this.message = message;
        this.isCompleted = isCompleted;
        this.linkedMetaUserId = linkedMetaUserId;
        this.completedAt = completedAt;
    }

    /**
     * 대기 중 상태 응답을 생성합니다.
     */
    public static PairingStatusResponse pending() {
        return PairingStatusResponse.builder()
                .status("PENDING")
                .message("VR 앱에서 페어링 코드를 스캔해주세요.")
                .isCompleted(false)
                .build();
    }

    /**
     * 완료 상태 응답을 생성합니다.
     */
    public static PairingStatusResponse completed(String metaUserId, LocalDateTime completedAt) {
        return PairingStatusResponse.builder()
                .status("COMPLETED")
                .message("페어링이 완료되었습니다. 아티스트 권한으로 승격되었습니다.")
                .isCompleted(true)
                .linkedMetaUserId(metaUserId)
                .completedAt(completedAt)
                .build();
    }

    /**
     * 만료 상태 응답을 생성합니다.
     */
    public static PairingStatusResponse expired() {
        return PairingStatusResponse.builder()
                .status("EXPIRED")
                .message("페어링 코드가 만료되었습니다. 새로운 코드를 생성해주세요.")
                .isCompleted(false)
                .build();
    }

    /**
     * 유효하지 않은 상태 응답을 생성합니다.
     */
    public static PairingStatusResponse invalid() {
        return PairingStatusResponse.builder()
                .status("INVALID")
                .message("유효하지 않은 페어링 코드입니다.")
                .isCompleted(false)
                .build();
    }
}
