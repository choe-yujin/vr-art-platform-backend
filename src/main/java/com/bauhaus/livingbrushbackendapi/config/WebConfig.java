package com.bauhaus.livingbrushbackendapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 웹 관련 설정을 통합 관리하는 클래스 (CORS, 정적 리소스 핸들러)
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 정의합니다.
     * 다른 도메인(예: localhost:3000, livingbrush.shop)에서의 API 요청을 허용합니다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 정책 적용
                .allowedOrigins("http://localhost:3000", "https://livingbrush.shop") // 허용할 출처
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 HTTP 메소드
                .allowedHeaders("*") // 모든 요청 헤더 허용
                .allowCredentials(true) // 쿠키 및 인증 정보 허용
                .maxAge(3600); // pre-flight 요청의 캐시 시간(초)
    }

    /**
     * 정적 리소스(Static Resource) 핸들러를 설정합니다.
     * 특정 URL 경로로 요청이 오면, 실제 로컬 파일 시스템의 특정 폴더에서 리소스를 찾아 제공합니다.
     * 이 설정 덕분에 생성된 QR 코드 이미지를 웹 브라우저에서 바로 볼 수 있습니다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // application.yml에 정의된 웹 경로 (예: /qr-images/)
        String webPath = appProperties.getQr().getWebPath();

        // application.yml에 정의된 실제 로컬 파일 시스템 경로를 절대 경로로 변환
        // (예: file:C:/Users/User/Java/project/src/main/static-files/qr-images/)
        String resourcePath = "file:" + Paths.get(appProperties.getQr().getLocalPath()).toAbsolutePath() + "/";

        // /qr-images/** 로 들어오는 모든 요청을 resourcePath 에서 찾도록 매핑합니다.
        registry.addResourceHandler(webPath + "**")
                .addResourceLocations(resourcePath);
    }
}