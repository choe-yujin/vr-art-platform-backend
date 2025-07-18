package com.bauhaus.livingbrushbackendapi.config;

import com.bauhaus.livingbrushbackendapi.security.JwtAuthenticationEntryPoint;
import com.bauhaus.livingbrushbackendapi.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정
 *
 * Android 앱 기반 JWT 인증 지원
 * - Google OAuth는 Android 앱에서 처리
 * - Spring Boot는 ID Token 검증 + JWT 발급만 담당
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Security Filter Chain 설정
     *
     * JWT 기반 인증만 지원 (웹 OAuth 제거)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 비활성화 (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증 예외 처리
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // URL별 인증 설정
                .authorizeHttpRequests(authorize -> authorize
                        // 공개 엔드포인트 (인증 불필요)
                        .requestMatchers(
                                "/api/auth/**",           // 인증 관련 API (Google 로그인)
                                "/api/artworks/public/**", // 공개 작품 조회
                                "/api/qr/**",             // QR 코드 스캔
                                "/health",                // 헬스체크
                                "/actuator/**",           // 모니터링
                                "/swagger-ui/**",         // Swagger UI
                                "/v3/api-docs/**",        // API 문서
                                "/h2-console/**"          // H2 Console (개발용)
                        ).permitAll()

                        // 개발 환경에서는 모든 요청 허용
                        .anyRequest().permitAll()
                );

        // H2 Console iframe 허용 (개발용)
        http.headers(headers -> headers.frameOptions().disable());

        // JWT 인증 필터 추가 (운영 환경에서 활성화)
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     *
     * VR/AR Android 앱에서 API 호출 허용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin (개발 환경에서는 모든 origin 허용)
        configuration.addAllowedOriginPattern("*");  // 모든 origin 허용 (개발용)

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.addAllowedHeader("*");  // 모든 헤더 허용

        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Number"
        ));

        // 자격 증명 허용
        configuration.setAllowCredentials(true);

        // 프리플라이트 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
