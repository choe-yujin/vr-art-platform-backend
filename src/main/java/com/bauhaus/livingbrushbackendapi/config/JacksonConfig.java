package com.bauhaus.livingbrushbackendapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 직렬화/역직렬화 설정
 * 
 * 안드로이드-백엔드 간 LocalDateTime 직렬화 호환성을 위한 설정
 * - LocalDateTime을 배열이 아닌 ISO 문자열로 직렬화
 * - 안드로이드가 기대하는 "2025-07-27T14:15:02" 형태로 변환
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Configuration
public class JacksonConfig {

    /**
     * ObjectMapper Bean 설정
     * 
     * 기존 문제:
     * - LocalDateTime이 [2025,7,27,14,15,2,692437000] 배열로 직렬화됨
     * - 안드로이드에서 문자열을 기대하여 파싱 오류 발생
     * 
     * 해결방법:
     * - write-dates-as-timestamps: false 설정
     * - JavaTimeModule과 LocalDateTimeSerializer 사용
     * - ISO 8601 형태의 문자열로 직렬화
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java Time API 지원 모듈 설정
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // LocalDateTime을 ISO 문자열로 직렬화
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        
        mapper.registerModule(javaTimeModule);
        
        // 타임스탬프 배열 대신 문자열 사용
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}
