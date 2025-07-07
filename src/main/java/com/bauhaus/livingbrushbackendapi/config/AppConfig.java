package com.bauhaus.livingbrushbackendapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        // 필요에 따라 커스텀 설정 추가 가능
        ObjectMapper mapper = new ObjectMapper();
        return mapper;
    }
}
