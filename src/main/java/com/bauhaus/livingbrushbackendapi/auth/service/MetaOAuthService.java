// src/main/java/com/bauhaus/livingbrushbackendapi/auth/service/MetaOAuthService.java
package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.MetaLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.MetaSignupRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.OAuthLoginRequest;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
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

    private final UserAccountLinkingService userAccountLinkingService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate metaRestTemplate;
    private final String metaUserInfoUri;

    // [수정] UserAccountLinkingService 추가
    public MetaOAuthService(
            UserAccountLinkingService userAccountLinkingService,
            JwtTokenProvider jwtTokenProvider,
            @Qualifier("metaRestTemplate") RestTemplate metaRestTemplate,
            @Value("${spring.security.oauth2.client.provider.meta.user-info-uri}") String metaUserInfoUri
    ) {
        this.userAccountLinkingService = userAccountLinkingService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.metaRestTemplate = metaRestTemplate;
        this.metaUserInfoUri = metaUserInfoUri;
    }

    // Meta API 응답을 받기 위한 내부 DTO
    private record MetaUserInfo(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("email") String email,
            @JsonProperty("picture") Picture picture
    ) {
        // 프로필 이미지 URL 추출 헬퍼 메서드
        public String getProfileImageUrl() {
            return (picture != null && picture.data() != null) ? picture.data().url() : null;
        }
        
        // Meta Graph API의 picture 응답 구조
        private record Picture(
                @JsonProperty("data") PictureData data
        ) {}
        
        private record PictureData(
                @JsonProperty("url") String url
        ) {}
    }

    @Override
    public Provider getProvider() {
        return Provider.META;
    }

    @Override
    @Transactional
    public AuthResponse authenticate(OAuthLoginRequest request) {
        String metaAccessToken;
        Platform platform;
        MetaSignupRequest.ConsentData consents = null;
        
        // 로그인과 회원가입 요청 구분 처리
        if (request instanceof MetaLoginRequest metaLoginRequest) {
            metaAccessToken = metaLoginRequest.metaAccessToken();
            platform = metaLoginRequest.getPlatform();
            log.info("Meta 로그인 요청 처리 - Platform: {}", platform);
        } else if (request instanceof MetaSignupRequest metaSignupRequest) {
            metaAccessToken = metaSignupRequest.metaAccessToken();
            platform = metaSignupRequest.getPlatform();
            consents = metaSignupRequest.consents();
            log.info("Meta 회원가입 요청 처리 - Platform: {}, Consents: STT={}, AI={}, DataTraining={}", 
                    platform, consents.sttConsent(), consents.aiConsent(), consents.dataTrainingConsent());
        } else {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "MetaLoginRequest 또는 MetaSignupRequest 타입이 필요합니다.");
        }

        // 1. Meta API에서 사용자 정보 조회
        MetaUserInfo metaUserInfo = verifyMetaUser(metaAccessToken);
        log.info("Meta 사용자 정보 확인 완료 - Meta User ID: {}, Platform: {}", metaUserInfo.id(), platform);

        // 2. UserAccountLinkingService를 통해 통합 계정 처리
        UserAccountLinkingService.OAuthAccountInfo accountInfo = UserAccountLinkingService.OAuthAccountInfo.builder()
                .provider("META")
                .providerUserId(metaUserInfo.id())
                .name(metaUserInfo.name())
                .email(metaUserInfo.email())
                .platform(platform.name())
                .profileImageUrl(metaUserInfo.getProfileImageUrl())
                .build();

        UserAccountLinkingService.AccountLinkingResult result = userAccountLinkingService.handleUnifiedAccountScenario(accountInfo);
        User user = result.getUser();
        
        // 3. 회원가입 요청인 경우 동의 정보 저장
        if (consents != null && result.getType() == UserAccountLinkingService.AccountLinkingType.NEW_USER_CREATED) {
            log.info("신규 사용자 동의 정보 저장 - User ID: {}", user.getUserId());
            user.getUserSettings().updateConsents(
                    consents.sttConsent(),
                    consents.aiConsent(),
                    consents.dataTrainingConsent()
            );
        }

        // 4. VR 플랫폼으로 접속했고, 아직 작가 권한이 없다면 작가로 승격
        if (platform == Platform.VR && user.getArtistQualifiedAt() == null) {
            log.info("사용자 {}를 ARTIST로 승격합니다.", user.getUserId());
            user.promoteToArtist();
        }

        // 5. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        String authType = consents != null ? "회원가입" : "로그인";
        log.info("Meta {} 성공 - User ID: {}, 처리 타입: {}", authType, user.getUserId(), result.getType());
        return new AuthResponse(accessToken, refreshToken, user.getUserId(), user.getRole());
    }

    /**
     * Meta Graph API를 호출하여 Access Token을 검증하고 사용자 정보를 가져옵니다.
     * @param accessToken Meta에서 발급한 Access Token
     * @return Meta 사용자 정보 (프로필 이미지 포함)
     */
    private MetaUserInfo verifyMetaUser(String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl(metaUserInfoUri)
                .queryParam("fields", "id,name,email,picture{url}") // picture 필드 추가
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