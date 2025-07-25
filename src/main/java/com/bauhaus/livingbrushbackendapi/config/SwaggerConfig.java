package com.bauhaus.livingbrushbackendapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI에서 JWT 인증이 자동으로 적용되도록 하는 설정
 * 
 * 모든 API에 JWT 토큰이 자동으로 포함되어 Execute 버튼만 눌러도 인증된 요청이 전송됩니다.
 */
@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME_NAME = "JWT";

    @Bean
    public OpenAPI openAPI() {
        // 1. API 정보를 설정합니다.
        Info info = new Info()
                .title("Livingbrush API Document")
                .version("v1.0.0")
                .description("Livingbrush 프로젝트의 API 명세서입니다.\n\n" +
                           "🔒 **인증 방법:**\n" +
                           "1. 우상단 'Authorize' 버튼 클릭\n" +
                           "2. JWT 토큰 입력 (Bearer 접두사 포함)\n" +
                           "3. 모든 API가 자동으로 인증 헤더 포함");

        // 2. JWT 인증 방식을 위한 SecurityScheme을 정의합니다.
        SecurityScheme jwtSecurityScheme = new SecurityScheme()
                .name(JWT_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.");

        // 3. 전역 SecurityRequirement 설정 - 모든 API에 적용
        SecurityRequirement globalSecurityRequirement = new SecurityRequirement()
                .addList(JWT_SCHEME_NAME);

        return new OpenAPI()
                .info(info)
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME_NAME, jwtSecurityScheme)
                )
                // 전역적으로 모든 API에 JWT 인증 적용
                .addSecurityItem(globalSecurityRequirement);
    }
}