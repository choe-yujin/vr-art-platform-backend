package com.bauhaus.livingbrushbackendapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * OpenAPI/Swagger 설정
 * 환경별로 서버 URL을 다르게 설정하여 CORS 문제를 해결합니다.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8888}")
    private int serverPort;

    @Bean
    @Profile("local")
    public OpenAPI localOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LivingBrush API")
                        .description("VR/AR 3D 작품 창작 플랫폼 API")
                        .version("1.0"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ));
    }

    @Bean
    @Profile("prod")
    public OpenAPI productionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LivingBrush API")
                        .description("VR/AR 3D 작품 창작 플랫폼 API")
                        .version("1.0"))
                .servers(List.of(
                        new Server()
                                .url("https://api.livingbrush.shop")
                                .description("Production Server (HTTPS)")
                ));
    }
}
