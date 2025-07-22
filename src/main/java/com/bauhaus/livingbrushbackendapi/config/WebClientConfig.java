package com.bauhaus.livingbrushbackendapi.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 설정
 * 
 * OAuth 프로필 이미지 다운로드 등 외부 HTTP 요청에 사용됩니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Configuration
public class WebClientConfig {

    /**
     * WebClient 빌더를 빈으로 등록합니다.
     * 
     * @return WebClient.Builder 인스턴스
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * 프로필 이미지 다운로드 전용 WebClient
     * 
     * 타임아웃과 버퍼 크기를 프로필 이미지 다운로드에 최적화
     * 
     * @return 설정된 WebClient 인스턴스
     */
    @Bean
    public WebClient profileImageWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10초 연결 타임아웃
            .responseTimeout(Duration.ofSeconds(15)) // 15초 응답 타임아웃
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB 버퍼
            .build();
    }
}
