package com.bauhaus.livingbrushbackendapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger OpenAPI 3.0 설정
 *
 * Android OAuth + JWT 인증 방식에 맞는 API 문서화
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8888}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(getApiInfo())
                .servers(getServers())
                .components(getComponents())
                .addSecurityItem(new SecurityRequirement().addList("JWT Bearer"));
    }

    /**
     * API 기본 정보
     */
    private Info getApiInfo() {
        return new Info()
                .title("LivingBrush Backend API")
                .description("""
                        🎨 **LivingBrush VR/AR 통합 플랫폼 API**
                        
                        ## 🔐 인증 방식
                        
                        ### Google OAuth (Android 앱)
                        1. VR/AR 앱에서 Google OAuth 로그인
                        2. ID Token을 `/api/auth/google-login`으로 전송
                        3. JWT Access Token 및 Refresh Token 수신
                        
                        ### JWT 인증
                        - **Header**: `Authorization: Bearer {access_token}`
                        - **유효시간**: 30분 (Access Token), 7일 (Refresh Token)
                        - **권한**: ARTIST (VR), VISITOR (AR), ADMIN
                        
                        ## 📱 지원 플랫폼
                        - **VR 앱**: Unity → Android APK (아티스트 권한)
                        - **AR 앱**: Unity → Android APK (관람객 권한)
                        
                        ## 🧪 테스트 방법
                        
                        ### 1. Google OAuth 시뮬레이션
                        ```bash
                        # 실제 환경에서는 Android 앱이 Google OAuth 처리
                        POST /api/auth/google-login
                        {
                          "idToken": "google_id_token_here",
                          "platform": "VR" or "AR"
                        }
                        ```
                        
                        ### 2. JWT 토큰 사용
                        - 위 API로 받은 `accessToken`을 Bearer 토큰으로 사용
                        - 우측 상단 🔒 버튼으로 인증 설정
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Bauhaus Team")
                        .email("dev@bauhaus.com")
                        .url("https://bauhaus.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * 서버 목록
     */
    private List<Server> getServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("개발 서버"),
                new Server()
                        .url("https://api.livingbrush.shop")
                        .description("프로덕션 서버")
        );
    }

    /**
     * 보안 스키마 설정
     */
    private Components getComponents() {
        return new Components()
                .addSecuritySchemes("JWT Bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                                **JWT Bearer Token 인증**
                                
                                1. `/api/auth/google-login`으로 Google OAuth 로그인
                                2. 응답받은 `accessToken`을 입력
                                3. 형식: `Bearer {token}` (Bearer는 자동 추가됨)
                                
                                **예시 토큰 (테스트용)**:
                                ```
                                eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEsInJvbGUiOiJBUlRJU1QiLCJpYXQiOjE2OTk5MTIzNDUsImV4cCI6MTY5OTkxNDE0NX0.example
                                ```
                                """));
    }
}
