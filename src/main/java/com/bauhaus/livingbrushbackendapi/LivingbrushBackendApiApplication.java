package com.bauhaus.livingbrushbackendapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Living Brush 백엔드 애플리케이션의 메인 클래스.
 *
 * @ConfigurationPropertiesScan: @ConfigurationProperties 어노테이션이 붙은 클래스들을
 * 자동으로 스캔하여 빈으로 등록합니다. (e.g., OAuthProperties)
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LivingbrushBackendApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LivingbrushBackendApiApplication.class, args);
    }
}