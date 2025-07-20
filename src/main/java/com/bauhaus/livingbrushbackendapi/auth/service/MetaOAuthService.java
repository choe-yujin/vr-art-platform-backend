// src/main/java/com/bauhaus/livingbrushbackendapi/auth/service/MetaOAuthService.java
package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.MetaLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.OAuthLoginRequest;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Meta (Oculus) OAuth 인증 서비스 (v2.2 최종)
 *
 * @author Bauhaus Team
 * @version 2.2
 */
@Slf4j
@Service
public class MetaOAuthService implements OAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate metaRestTemplate;
    private final String metaUserInfoUri;

    // [수정] @RequiredArgsConstructor를 제거하고, 명시적인 생성자를 사용합니다.
    // 이렇게 해야 @Qualifier와 @Value가 올바르게 동작합니다.
    public MetaOAuthService(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            @Qualifier("metaRestTemplate") RestTemplate metaRestTemplate, // "metaRestTemplate" 별명을 가진 Bean을 주입
            @Value("${spring.security.oauth2.client.provider.meta.user-info-uri}") String metaUserInfoUri // yml의 값을 주입
    ) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.metaRestTemplate = metaRestTemplate;
        this.metaUserInfoUri = metaUserInfoUri;
    }

    // Meta API 응답을 받기 위한 내부 DTO
    private record MetaUserInfo(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("email") String email
    ) {}

    @Override
    public Provider getProvider() {
        return Provider.META;
    }

    @Override
    @Transactional
    public AuthResponse authenticate(OAuthLoginRequest request) {
        if (!(request instanceof MetaLoginRequest metaRequest)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "MetaLoginRequest 타입이 필요합니다.");
        }

        MetaUserInfo metaUserInfo = verifyMetaUser(metaRequest.metaAccessToken());
        log.info("Meta 사용자 정보 확인 완료 - Meta User ID: {}, Platform: {}", metaUserInfo.id(), metaRequest.getPlatform());

        // 기존 사용자를 찾거나, 없으면 새로 생성합니다.
        User user = userRepository.findByMetaUserId(metaUserInfo.id())
                .orElseGet(() -> {
                    log.info("신규 Meta 사용자 등록 - Meta User ID: {}", metaUserInfo.id());
                    return userRepository.save(
                            User.createNewMetaUser(metaUserInfo.id(), metaUserInfo.email(), metaUserInfo.name())
                    );
                });

        // 최신 정보로 프로필을 업데이트합니다.
        user.updateProfile(metaUserInfo.name(), metaUserInfo.email());

        // VR 플랫폼으로 접속했고, 아직 작가 권한이 없다면 작가로 승격시킵니다.
        if (metaRequest.getPlatform() == Platform.VR && user.getArtistQualifiedAt() == null) {
            log.info("사용자 {}를 ARTIST로 승격합니다.", user.getUserId());
            user.promoteToArtist();
        }

        // JWT 토큰을 생성합니다.
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        return new AuthResponse(accessToken, refreshToken, user.getUserId(), user.getRole());
    }

    /**
     * Meta Graph API를 호출하여 Access Token을 검증하고 사용자 정보를 가져옵니다.
     * @param accessToken Meta에서 발급한 Access Token
     * @return Meta 사용자 정보
     */
    private MetaUserInfo verifyMetaUser(String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl(metaUserInfoUri)
                .queryParam("fields", "id,name,email")
                .queryParam("access_token", accessToken)
                .toUriString();
        try {
            ResponseEntity<MetaUserInfo> response = metaRestTemplate.getForEntity(url, MetaUserInfo.class);
            MetaUserInfo userInfo = response.getBody();

            if (userInfo == null || userInfo.id() == null) {
                log.error("Meta로부터 잘못된 응답 수신: {}", response.getBody());
                throw new CustomException(ErrorCode.OAUTH_PROVIDER_ERROR, "Meta로부터 유효한 사용자 정보를 받지 못했습니다.");
            }
            return userInfo;
        } catch (HttpClientErrorException e) {
            log.error("Meta 토큰 검증 실패: Status {}, Body {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Meta Access Token 입니다.");
        }
    }
}