package com.bauhaus.livingbrushbackendapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 * 
 * VR 로그인 임시 토큰 저장/조회를 위한 RedisTemplate 설정을 제공합니다.
 * 기존 application.yml의 Redis 설정을 활용하여 최소한의 구성으로 운영됩니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Configuration
public class RedisConfig {

    /**
     * VR 로그인 토큰 관리를 위한 RedisTemplate 빈을 생성합니다.
     * 
     * 주요 특징:
     * - Key/Value 모두 String 타입으로 단순화
     * - TTL(Time To Live) 지원으로 자동 만료 처리
     * - VR 토큰 저장 형식: "vr_login:{uuid}" -> "{userId}"
     * 
     * @param connectionFactory Spring Boot가 자동 구성한 Redis 연결 팩토리
     * @return VR 로그인 토큰 관리용 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key와 Value 모두 String 직렬화 사용 (단순하고 안전함)
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        
        // Hash 직렬화 설정 (나중에 확장 시 사용 가능)
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}