package com.bauhaus.livingbrushbackendapi.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;

/**
 * 애플리케이션의 핵심 Bean 설정을 담당합니다. (v2.0 - Modernized)
 *
 * @version 2.0
 */
@Configuration
public class AppConfig {

    /**
     * 애플리케이션 전역에서 사용될 기본 RestTemplate을 생성합니다.
     * Spring Boot가 제공하는 RestTemplateBuilder를 사용하여 타임아웃 설정을 간소화합니다.
     *
     * @param builder Spring Boot에 의해 자동 구성된 RestTemplateBuilder
     * @return 타임아웃이 설정된 RestTemplate 인스턴스
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5)); // 연결 타임아웃
        requestFactory.setReadTimeout(Duration.ofSeconds(5));    // 읽기 타임아웃

        return builder
                .requestFactory(() -> requestFactory)
                .build();
    }

    /**
     * Google ID Token 검증을 위한 GoogleIdTokenVerifier Bean을 생성합니다.
     * [개선] Spring Security 표준 OAuth2 설정인 'spring.security.oauth2.client.registration.google.client-id'를 사용합니다.
     *
     * @param googleClientId application.yml에 정의된 Google Client ID (${GOOGLE_CLIENT_ID})
     * @return 설정이 완료된 GoogleIdTokenVerifier 인스턴스
     */
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId
    ) {
        System.out.println("🔧 GoogleIdTokenVerifier 생성 - Client ID: " + googleClientId);
        
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                // Audience는 Client ID 목록입니다.
                .setAudience(Collections.singletonList(googleClientId))
                // Issuer는 Google의 경우 고정된 값입니다.
                .setIssuer("https://accounts.google.com")
                .build();
    }

    /*
     * [개선] ObjectMapper Bean은 더 이상 필요하지 않습니다.
     *
     * 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310' 의존성이 프로젝트에 존재하면,
     * Spring Boot가 자동으로 Java 8의 시간 타입(LocalDate, LocalDateTime 등)을
     * JSON으로 변환할 수 있는 ObjectMapper를 Bean으로 등록해줍니다.
     *
     * 따라서 이 Bean을 직접 생성하지 않는 것이 Spring Boot의 자동 구성을 최대한 활용하는 방법입니다.
     */
    // @Bean
    // public ObjectMapper objectMapper() {
    //     ObjectMapper mapper = new ObjectMapper();
    //     mapper.registerModule(new JavaTimeModule());
    //     return mapper;
    // }
}