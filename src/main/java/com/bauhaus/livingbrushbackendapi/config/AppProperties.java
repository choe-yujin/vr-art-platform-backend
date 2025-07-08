package com.bauhaus.livingbrushbackendapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@Validated // 설정 값 유효성 검증 활성화
public class AppProperties {

    private final Qr qr = new Qr();
    private final WebAr webAr = new WebAr();

    @Getter
    @Setter
    public static class Qr {
        @NotBlank
        private String localPath;
        @Positive
        private int size;
        @NotBlank
        private String format;
    }

    @Getter
    @Setter
    public static class WebAr {
        @NotBlank
        private String baseUrl;
    }
}