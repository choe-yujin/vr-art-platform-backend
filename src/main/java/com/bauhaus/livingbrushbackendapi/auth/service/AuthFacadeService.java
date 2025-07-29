package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.GoogleLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.MetaLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.MetaSignupRequest;
// AccountLinkingResult를 반환하기 위해 import 합니다.
import com.bauhaus.livingbrushbackendapi.auth.service.UserAccountLinkingService.AccountLinkingResult;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 다양한 인증 서비스를 묶어주는 퍼사드(Facade) 서비스입니다.
 * 컨트롤러는 이 퍼사드를 통해 일관된 방식으로 인증을 요청합니다.
 * [개선] 모든 인증 메소드가 AccountLinkingResult를 반환하도록 통일합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeService {

    private final GoogleAuthService googleAuthService;
    private final MetaOAuthService metaOAuthService;

    /**
     * 동의가 필요한 회원가입 시나리오를 처리합니다. (현재는 Meta만 해당)
     * [수정] 컨트롤러의 기대에 맞게 AuthResponse 대신 AccountLinkingResult를 반환합니다.
     *
     * @param provider 인증 제공자 (META)
     * @param request  요청 DTO
     * @return 계정 처리 결과 (로그인, 연동, 신규 생성)
     */
    public AccountLinkingResult authenticateWithConsents(Provider provider, MetaSignupRequest request) {
        if (provider == Provider.META) {
            // 이제 MetaOAuthService도 AccountLinkingResult를 반환해야 합니다.
            return metaOAuthService.authenticateWithConsents(request);
        }
        log.error("Unsupported provider for signup with consents: {}", provider);
        throw new IllegalArgumentException("동의가 필요한 회원가입은 현재 Meta만 지원합니다: " + provider);
    }

    /**
     * 일반적인 로그인/회원가입 시나리오를 처리합니다.
     * [수정] 컨트롤러의 기대에 맞게 AuthResponse 대신 AccountLinkingResult를 반환합니다.
     *
     * @param provider   인증 제공자 (GOOGLE, META)
     * @param requestDto 요청 DTO
     * @return 계정 처리 결과 (로그인, 연동, 신규 생성)
     */
    public AccountLinkingResult authenticate(Provider provider, Object requestDto) {
        log.info("Authenticating with provider: {}", provider);
        // 이 메소드를 호출받는 GoogleAuthService와 MetaOAuthService도
        // 모두 AccountLinkingResult를 반환하도록 수정되어야 합니다.
        return switch (provider) {
            case GOOGLE -> googleAuthService.authenticate((GoogleLoginRequest) requestDto);
            case META -> metaOAuthService.authenticate((MetaLoginRequest) requestDto);
            default -> {
                log.error("Unsupported provider for authentication: {}", provider);
                throw new IllegalArgumentException("지원하지 않는 인증 제공자입니다: " + provider);
            }
        };
    }
}