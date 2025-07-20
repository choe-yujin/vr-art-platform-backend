package com.bauhaus.livingbrushbackendapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // 1. API 정보를 설정합니다.
        Info info = new Info()
                .title("Livingbrush API Document")
                .version("v1.0.0")
                .description("Livingbrush 프로젝트의 API 명세서입니다.");

        // 2. JWT 인증 방식을 위한 SecurityScheme을 정의합니다.
        String jwtSchemeName = "jwtAuth";
        SecurityScheme securityScheme = new SecurityScheme()
                .name(jwtSchemeName)
                .type(SecurityScheme.Type.HTTP) // HTTP 방식
                .scheme("bearer") // bearer 토큰 방식을 사용
                .bearerFormat("JWT"); // 토큰 형식은 JWT

        // 3. 모든 API에 전역적으로 인증을 적용하기 위한 SecurityRequirement를 추가합니다.
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(jwtSchemeName, securityScheme))
                .addSecurityItem(securityRequirement)
                .info(info);
    }
}