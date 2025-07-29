package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.MetaLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.MetaSignupRequest;
// AccountLinkingResult를 반환하기 위해 import 합니다.
import com.bauhaus.livingbrushbackendapi.auth.service.UserAccountLinkingService.AccountLinkingResult;
import com.bauhaus.livingbrushbackendapi.auth.service.UserAccountLinkingService.OAuthAccountInfo;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaOAuthService {

    private static final String META_GRAPH_API_URL = "https://graph.facebook.com/v18.0/me?fields=id,name,email,picture";
    private final RestTemplate restTemplate;
    private final UserAccountLinkingService userAccountLinkingService;

    /**
     * Meta Access Token을 사용하여 사용자를 인증합니다. (로그인 전용)
     * [수정] AuthResponse 대신 AccountLinkingResult를 반환하여 컨트롤러가 최종 응답을 생성하도록 책임을 위임합니다.
     *
     * @param request Meta 로그인 요청 DTO
     * @return 계정 처리 결과 (로그인, 연동, 신규 생성)
     */
    @Transactional
    public AccountLinkingResult authenticate(MetaLoginRequest request) {
        return processMetaAuthentication(request.metaAccessToken(), request.getPlatform().name(), null);
    }

    /**
     * 동의 정보가 포함된 Meta Access Token으로 사용자를 인증합니다. (회원가입 전용)
     * [수정] AuthResponse 대신 AccountLinkingResult를 반환하여 컨트롤러가 최종 응답을 생성하도록 책임을 위임합니다.
     *
     * @param request Meta 회원가입 요청 DTO
     * @return 계정 처리 결과 (로그인, 연동, 신규 생성)
     */
    @Transactional
    public AccountLinkingResult authenticateWithConsents(MetaSignupRequest request) {
        // MetaSignupRequest에서 필요한 정보를 추출하여 processMetaAuthentication 호출
        return processMetaAuthentication(request.metaAccessToken(), request.platform().name(), request.consents());
    }

    /**
     * Meta 인증의 공통 로직을 처리합니다.
     *
     * @param accessToken Meta Access Token
     * @param platform    요청 플랫폼 (AR, VR)
     * @param consents    동의 정보 (회원가입 시에만 사용, 로그인 시 null)
     * @return 계정 처리 결과
     */
    private AccountLinkingResult processMetaAuthentication(String accessToken, String platform, Object consents) {
        // 1. Meta Graph API를 호출하여 사용자 정보 조회
        Map<String, Object> metaUserAttributes = getMetaUserAttributes(accessToken);

        // 2. 조회된 정보로 OAuthAccountInfo 객체 생성
        OAuthAccountInfo accountInfo = buildOAuthAccountInfo(metaUserAttributes, platform);

        String authType = consents != null ? "회원가입" : "로그인";
        log.info("Meta {} 요청 처리 시작 - Meta User ID: {}, Platform: {}", authType, accountInfo.getProviderUserId(), platform);

        // 3. 계정 처리 서비스에 위임하고, 그 결과를 그대로 반환
        return userAccountLinkingService.handleUnifiedAccountScenario(accountInfo);
    }

    private Map<String, Object> getMetaUserAttributes(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(META_GRAPH_API_URL, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.warn("Meta Graph API 호출 실패: {}", response.getStatusCode());
                throw new SecurityException("Meta 사용자 정보를 가져오는 데 실패했습니다. 상태 코드: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Meta Graph API 호출 중 오류 발생", e);
            throw new SecurityException("Meta Graph API 호출 중 오류가 발생했습니다.", e);
        }
    }

    private OAuthAccountInfo buildOAuthAccountInfo(Map<String, Object> attributes, String platform) {
        String providerUserId = (String) attributes.get("id");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        String profileImageUrl = null;
        if (attributes.get("picture") instanceof Map pictureMap) {
            if (pictureMap.get("data") instanceof Map dataMap) {
                profileImageUrl = (String) dataMap.get("url");
            }
        }

        return OAuthAccountInfo.builder()
                .provider(Provider.META.name())
                .providerUserId(providerUserId)
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .platform(platform)
                .build();
    }
}