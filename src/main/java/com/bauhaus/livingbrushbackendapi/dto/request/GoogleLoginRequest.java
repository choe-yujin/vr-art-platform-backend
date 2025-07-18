package com.bauhaus.livingbrushbackendapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google 로그인 요청 DTO
 * 
 * Android 앱에서 Google OAuth로 받은 ID Token을 전송할 때 사용
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID Token은 필수입니다")
    private String idToken;

    @NotBlank(message = "플랫폼 정보는 필수입니다")
    private String platform; // "VR" 또는 "AR"

    @Builder
    private GoogleLoginRequest(String idToken, String platform) {
        this.idToken = idToken;
        this.platform = platform;
    }
}
