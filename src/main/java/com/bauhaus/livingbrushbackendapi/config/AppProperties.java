package com.bauhaus.livingbrushbackendapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 전반의 설정값을 관리하는 클래스 (리팩토링 v2.1)
 * app.* 에 해당하는 모든 설정을 이 클래스에서 중앙 관리합니다.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Qr qr = new Qr();
    private final WebAr webAr = new WebAr(); // [추가] web-ar 설정을 위한 필드

    /**
     * QR 코드 관련 설정 (app.qr.*)
     */
    @Getter
    @Setter
    public static class Qr {
        private String localPath;
        private String webPath;
        private int size;
        private String format; // [추가] QR 이미지 포맷 설정
    }

    /**
     * Web AR 관련 설정 (app.web-ar.*)
     */
    @Getter
    @Setter
    public static class WebAr { // [추가] web-ar 설정을 위한 중첩 클래스
        private String baseUrl;
    }
}