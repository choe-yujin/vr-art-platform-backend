package com.bauhaus.livingbrushbackendapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정 클래스
 * 
 * 로컬 개발 환경에서 static-files 서빙과 CORS 설정을 담당합니다.
 * QR 이미지와 샘플 파일들에 대한 HTTP 접근을 가능하게 합니다.
 */
@Slf4j
@Configuration
@Profile("local")
public class WebConfig implements WebMvcConfigurer {

    /**
     * Static Resource Handler 설정
     * 
     * /static-files/** 요청을 src/main/static-files/ 디렉토리로 매핑합니다.
     * QR 이미지, 샘플 .glb 파일, 썸네일 등에 HTTP로 접근 가능합니다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("Static resource handlers 설정 중...");
        
        // QR 이미지 및 샘플 파일 서빙
        registry.addResourceHandler("/static-files/**")
                .addResourceLocations("file:src/main/static-files/")
                .setCachePeriod(0); // 개발 환경에서는 캐시 비활성화
        
        log.info("Static files 매핑 완료: /static-files/** -> file:src/main/static-files/");
    }

    /**
     * CORS 설정 (개발 환경용)
     * 
     * 로컬 개발시 프론트엔드와 백엔드 포트가 다를 때 
     * CORS 오류를 방지하기 위한 설정입니다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("CORS 설정 중 (로컬 개발용)...");
        
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        
        // Static files에 대한 CORS 허용
        registry.addMapping("/static-files/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET")
                .allowedHeaders("*")
                .maxAge(3600);
        
        log.info("CORS 설정 완료 - 모든 origin 허용 (개발용)");
    }
}
