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
        private String localPath = "src/main/static-files/qr-images";
        @Positive
        private int size = 300;
        @NotBlank
        private String format = "PNG";
    }

    @Getter
    @Setter
    public static class WebAr {
        @NotBlank
        private String baseUrl = "https://bauhaus.shop/ar/view";
    }
}