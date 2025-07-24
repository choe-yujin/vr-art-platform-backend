package com.bauhaus.livingbrushbackendapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 * 
 * VR 로그인용 임시 토큰 저장 및 관리를 위한 Redis 연동 설정
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    /**
     * Redis 연결 팩토리 설정
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        // 비밀번호가 설정된 경우에만 적용
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        return new LettuceConnectionFactory(config);
    }

    /**
     * VR 로그인 토큰 관리용 Redis 템플릿
     * 
     * Key: "vr_login:{uuid-token}"
     * Value: "{userId}"
     * TTL: 300초 (5분)
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // String 직렬화 사용 (JSON보다 가벼움)
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        // 기본 직렬화 설정
        template.setDefaultSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        
        return template;
    }
}
