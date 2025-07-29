package com.bauhaus.livingbrushbackendapi.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 애플리케이션 전반의 설정값을 관리하는 클래스 (리팩토링 v2.2)
 * app.* 에 해당하는 모든 설정을 이 클래스에서 중앙 관리하며, 유효성을 검증합니다.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@Validated // [개선] 이 클래스의 필드에 설정된 유효성 검증(e.g., @NotBlank)을 활성화합니다.
public class AppProperties {

    @Valid // [개선] 중첩된 클래스에 대해서도 유효성 검증을 수행하도록 지정합니다.
    private final Qr qr = new Qr();

    @Valid
    private final WebAr webAr = new WebAr();

    @Valid
    private final Profile profile = new Profile(); // [추가] 프로필 관련 설정을 위한 필드

    /**
     * QR 코드 관련 설정 (app.qr.*)
     */
    @Getter
    @Setter
    public static class Qr {
        @NotBlank
        private String localPath;
        @NotBlank
        private String webPath;
        private int size;
        @NotBlank
        private String format;
    }

    /**
     * Web AR 관련 설정 (app.web-ar.*)
     */
    @Getter
    @Setter
    public static class WebAr {
        @NotBlank
        private String baseUrl;
    }

    /**
     * [추가] 프로필 관련 설정 (app.profile.*)
     */
    @Getter
    @Setter
    public static class Profile {
        /**
         * 사용자가 프로필 이미지를 설정하지 않았을 때 사용될 기본 이미지 URL입니다.
         * 이 값이 비어있거나 null이면 애플리케이션 시작 시 오류를 발생시켜 설정 누락을 방지합니다.
         */
        @NotBlank
        private String defaultImageUrl;
    }
}