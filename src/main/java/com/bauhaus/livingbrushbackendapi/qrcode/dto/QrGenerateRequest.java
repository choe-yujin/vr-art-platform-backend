package com.bauhaus.livingbrushbackendapi.qrcode.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * QR 코드 생성 요청 DTO
 * 
 * 공개 작품에 대해 QR 코드를 생성하기 위한 요청 데이터
 * 작품 ID 기반으로 QR 토큰과 이미지를 생성합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QrGenerateRequest {

    @NotNull(message = "작품 ID는 필수입니다")
    private Long artworkId;

    @Builder
    private QrGenerateRequest(Long artworkId) {
        this.artworkId = artworkId;
    }
}
