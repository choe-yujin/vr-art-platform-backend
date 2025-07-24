package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.OAuthLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.MetaSignupRequest;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthFacadeService {

    private final Map<Provider, OAuthService> serviceMap;

    // Spring이 OAuthService를 구현하는 모든 Bean을 주입하면, Map으로 변환하여 저장
    public AuthFacadeService(List<OAuthService> oAuthServices) {
        this.serviceMap = oAuthServices.stream()
                .collect(Collectors.toUnmodifiableMap(OAuthService::getProvider, Function.identity()));
    }

    /**
     * Provider 타입에 맞는 OAuth 서비스를 찾아 인증을 위임합니다.
     * @param provider 인증 제공자 (GOOGLE, META 등)
     * @param request 로그인 요청 DTO
     * @return 인증 결과
     */
    public AuthResponse authenticate(Provider provider, OAuthLoginRequest request) {
        OAuthService service = serviceMap.get(provider);
        if (service == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 로그인 방식입니다: " + provider);
        }
        return service.authenticate(request);
    }

    /**
     * 동의 정보와 함께 회원가입/로그인을 처리합니다.
     * MetaSignupRequest의 경우 동의 정보를 UserSetting에 저장합니다.
     * @param provider 인증 제공자 (META만 지원)
     * @param request 회원가입 요청 DTO (동의 정보 포함)
     * @return 인증 결과
     */
    public AuthResponse authenticateWithConsents(Provider provider, MetaSignupRequest request) {
        if (provider != Provider.META) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "동의 정보를 포함한 회원가입은 Meta만 지원합니다.");
        }
        
        OAuthService service = serviceMap.get(provider);
        if (service == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 로그인 방식입니다: " + provider);
        }
        
        // MetaSignupRequest를 OAuthLoginRequest로 캐스팅하여 기존 로직 활용
        return service.authenticate(request);
    }
}