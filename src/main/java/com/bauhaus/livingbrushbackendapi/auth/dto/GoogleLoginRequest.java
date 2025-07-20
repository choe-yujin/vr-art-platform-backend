package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Google 로그인 요청 DTO (리팩토링 v2.2)
 *
 * Java record를 사용하여 불변(immutable)하고 간결한 데이터 객체로 정의하며,
 * OAuthLoginRequest 인터페이스를 구현하여 다형성을 지원합니다.
 *
 * @author Bauhaus Team
 * @version 2.2
 */
public record GoogleLoginRequest(
        @NotBlank(message = "Google ID Token은 필수입니다.")
        String idToken,

        @NotNull(message = "플랫폼 정보는 필수입니다.")
        Platform platform
) implements OAuthLoginRequest {

        /**
         * OAuthLoginRequest 인터페이스의 getPlatform() 메소드를 명시적으로 구현합니다.
         * record의 'platform' 컴포넌트 값을 반환하여 인터페이스 계약을 준수합니다.
         */
        @Override
        public Platform getPlatform() {
                return this.platform;
        }
}