package com.bauhaus.livingbrushbackendapi.security;

import com.bauhaus.livingbrushbackendapi.security.jwt.JwtAccessDeniedHandler;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtAuthenticationEntryPoint;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정 클래스 (v3.0 - Production Ready)
 *
 * - Lambda DSL을 사용하여 최신 설정 스타일 적용
 * - JWT 필터, EntryPoint, AccessDeniedHandler를 등록하여 인증/인가 처리 통합
 * - CORS(Cross-Origin Resource Sharing) 정책 설정 추가
 * - PasswordEncoder Bean 등록
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // [수정] 인증 없이 접근 가능한 경로 목록에 개발용 API 경로 추가
    private static final String[] PUBLIC_URLS = {
            // --- Basic and Error ---
            "/", "/error",

            // --- Swagger UI v3 ---
            "/swagger-ui/**", "/v3/api-docs/**",

            // --- Authentication ---
            "/api/auth/**", // 로그인, health check 등 인증 관련 모든 경로 포함

            // --- Development ---
            "/api/dev/**", // 개발용 테스트 API 경로

            // --- Static Resources ---
            "/qr-images/**" // QR 코드 이미지 경로
    };

    /**
     * Spring Security의 핵심 필터 체인을 설정합니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. CSRF 비활성화 (Stateless API는 CSRF 보호가 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Form 로그인 및 HTTP Basic 인증 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 4. 세션 관리 정책: STATELESS (세션을 사용하지 않음)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. 요청 경로별 인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll() // 공개 경로는 모두 허용
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )

                // 6. 커스텀 예외 처리 핸들러 등록
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                // 7. 커스텀 JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * [추가] CORS(Cross-Origin Resource Sharing) 설정을 정의합니다.
     * 프론트엔드 서버(React, Vue 등)의 주소를 허용하여 API 통신이 가능하도록 합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // TODO: 운영 환경에서는 실제 프론트엔드 도메인을 명시해야 합니다.
        // 예: "https://livingbrush.com"
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 자격 증명(쿠키 등) 허용
        configuration.setMaxAge(3600L); // pre-flight 요청 캐시 시간(초)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 위 설정 적용
        return source;
    }

    /**
     * [추가] 비밀번호 암호화를 위한 PasswordEncoder를 Bean으로 등록합니다.
     * Spring Security는 암호화 방식의 표준으로 BCrypt를 권장합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}