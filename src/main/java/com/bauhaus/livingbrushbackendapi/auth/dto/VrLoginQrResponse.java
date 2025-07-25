package com.bauhaus.livingbrushbackendapi.auth.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AR 앱에서 VR 로그인용 QR 코드 생성 응답 DTO
 * 
 * VR 기기에서 스캔할 QR 이미지와 토큰 정보를 제공합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VrLoginQrResponse {

    /**
     * 생성된 QR 코드 이미지 URL
     * S3 또는 로컬 스토리지에 저장된 QR 이미지 경로
     */
    private String qrImageUrl;

    /**
     * VR 로그인용 임시 토큰 (UUID 형태)
     * Redis에 저장되며 5분 후 자동 만료
     */
    private String vrLoginToken;

    /**
     * VR 수동 입력용 4자리 숫자 코드
     * 카메라를 사용할 수 없는 VR 기기에서 키보드로 직접 입력
     * 예: "1234", "0987"
     */
    private String manualCode;

    /**
     * 토큰 만료 시간 (5분 후)
     * 클라이언트에서 만료 시간을 표시하기 위해 제공
     */
    private LocalDateTime expiresAt;

    /**
     * 토큰 유효 시간 (초 단위)
     * 클라이언트에서 카운트다운을 위해 제공
     */
    private int expiresInSeconds;

    @Builder
    private VrLoginQrResponse(String qrImageUrl, String vrLoginToken, String manualCode, LocalDateTime expiresAt, int expiresInSeconds) {
        this.qrImageUrl = qrImageUrl;
        this.vrLoginToken = vrLoginToken;
        this.manualCode = manualCode;
        this.expiresAt = expiresAt;
        this.expiresInSeconds = expiresInSeconds;
    }

    /**
     * 성공적인 VR QR 생성 응답을 생성합니다.
     * 
     * @param qrImageUrl 생성된 QR 이미지 URL
     * @param vrLoginToken VR 로그인 토큰
     * @param manualCode 4자리 숫자 수동 입력 코드
     * @return VR QR 응답 객체
     */
    public static VrLoginQrResponse of(String qrImageUrl, String vrLoginToken, String manualCode) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        return VrLoginQrResponse.builder()
                .qrImageUrl(qrImageUrl)
                .vrLoginToken(vrLoginToken)
                .manualCode(manualCode)
                .expiresAt(expiresAt)
                .expiresInSeconds(300) // 5분 = 300초
                .build();
    }
}