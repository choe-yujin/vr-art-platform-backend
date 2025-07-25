package com.bauhaus.livingbrushbackendapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * VR 기기에서 QR 토큰으로 로그인 요청 DTO
 * 
 * QR 코드에서 스캔한 토큰으로 즉시 로그인을 처리합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VrLoginRequest {

    /**
     * VR 로그인용 임시 토큰
     * QR 코드에서 스캔한 UUID 형태의 토큰
     */
    @NotBlank(message = "VR 로그인 토큰은 필수입니다")
    private String vrLoginToken;

    @Builder
    private VrLoginRequest(String vrLoginToken) {
        this.vrLoginToken = vrLoginToken;
    }

    /**
     * VR 로그인 요청 객체를 생성합니다.
     * 
     * @param vrLoginToken VR 로그인 토큰
     * @return VR 로그인 요청 객체
     */
    public static VrLoginRequest of(String vrLoginToken) {
        return VrLoginRequest.builder()
                .vrLoginToken(vrLoginToken)
                .build();
    }
}