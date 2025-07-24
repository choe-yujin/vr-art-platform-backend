package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Meta (Oculus) 회원가입 요청 DTO
 * 
 * Meta Access Token과 함께 개인정보 및 AI 기능 사용 동의 정보를 포함합니다.
 * VR 앱에서 신규 사용자 회원가입 시 사용됩니다.
 *
 * @author Bauhaus Team
 * @version 1.0
 */
public record MetaSignupRequest(
        @NotBlank(message = "Meta Access Token is required.")
        String metaAccessToken,

        @NotNull(message = "Platform information is required.")
        Platform platform,

        @Valid
        @NotNull(message = "Consent information is required.")
        ConsentData consents
) implements OAuthLoginRequest {

        /**
         * OAuthLoginRequest 인터페이스 구현
         */
        @Override
        public Platform getPlatform() {
                return this.platform;
        }

        /**
         * 동의 정보 DTO
         * VR 앱 회원가입 시 필요한 모든 동의 항목을 포함합니다.
         */
        public record ConsentData(
                @NotNull(message = "STT consent is required.")
                Boolean sttConsent,

                @NotNull(message = "AI consent is required.")
                Boolean aiConsent,

                @NotNull(message = "Data training consent is required.")
                Boolean dataTrainingConsent
        ) {
                /**
                 * 필수 동의 항목 검증
                 * STT와 AI 기능 동의는 VR 앱 사용을 위한 필수 항목입니다.
                 * 
                 * @return 필수 동의 완료 여부
                 */
                public boolean areRequiredConsentsProvided() {
                        return Boolean.TRUE.equals(sttConsent) && Boolean.TRUE.equals(aiConsent);
                }

                /**
                 * 모든 동의 항목 완료 여부
                 * 
                 * @return 모든 동의 완료 여부 (선택 항목 포함)
                 */
                public boolean areAllConsentsProvided() {
                        return Boolean.TRUE.equals(sttConsent) && 
                               Boolean.TRUE.equals(aiConsent) && 
                               Boolean.TRUE.equals(dataTrainingConsent);
                }
        }
}
