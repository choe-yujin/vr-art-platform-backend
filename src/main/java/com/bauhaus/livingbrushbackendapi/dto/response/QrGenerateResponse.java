package com.bauhaus.livingbrushbackendapi.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * QR 코드 생성 응답 DTO
 * 
 * QR 코드 생성 성공 후 클라이언트에게 반환되는 데이터
 * AR 앱에서 QR 이미지를 화면에 표시하기 위한 URL을 제공합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QrGenerateResponse {

    private String qrImageUrl;

    @Builder
    private QrGenerateResponse(String qrImageUrl) {
        this.qrImageUrl = qrImageUrl;
    }

    /**
     * QR 이미지 URL로부터 응답 객체를 생성하는 정적 팩토리 메서드
     * 
     * @param qrImageUrl QR 이미지가 저장된 URL (로컬 또는 S3)
     * @return QrGenerateResponse 인스턴스
     */
    public static QrGenerateResponse of(String qrImageUrl) {
        return QrGenerateResponse.builder()
                .qrImageUrl(qrImageUrl)
                .build();
    }
}
