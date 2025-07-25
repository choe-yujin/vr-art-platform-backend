package com.bauhaus.livingbrushbackendapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * VR 기기에서 4자리 숫자 코드로 수동 로그인 요청 DTO
 * 
 * 카메라 사용이 어려운 VR 환경에서 키보드로 4자리 숫자를 직접 입력하여 로그인합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VrManualLoginRequest {

    /**
     * VR 수동 입력용 4자리 숫자 코드
     * 0000~9999 범위의 4자리 숫자만 허용
     */
    @NotBlank(message = "수동 입력 코드는 필수입니다")
    @Pattern(regexp = "^\\d{4}$", message = "수동 입력 코드는 4자리 숫자여야 합니다")
    private String manualCode;

    @Builder
    private VrManualLoginRequest(String manualCode) {
        this.manualCode = manualCode;
    }

    /**
     * VR 수동 로그인 요청 객체를 생성합니다.
     * 
     * @param manualCode 4자리 숫자 코드
     * @return VR 수동 로그인 요청 객체
     */
    public static VrManualLoginRequest of(String manualCode) {
        return VrManualLoginRequest.builder()
                .manualCode(manualCode)
                .build();
    }
}