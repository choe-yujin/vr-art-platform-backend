package com.bauhaus.livingbrushbackendapi.qrcode.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * QR 코드 생성 응답 DTO입니다.
 * Java record를 사용하여 불변하고 간결한 데이터 객체로 정의합니다.
 */
@Schema(description = "QR 코드 생성 응답 DTO")
public record QrGenerateResponse(
        @Schema(description = "생성된 QR 코드 이미지에 접근할 수 있는 전체 URL", example = "https://storage.googleapis.com/your-bucket/qr-codes/artwork_123.png")
        String qrImageUrl
) {
    // record는 자동으로 public String qrImageUrl() 접근자 메소드를 생성합니다.
}