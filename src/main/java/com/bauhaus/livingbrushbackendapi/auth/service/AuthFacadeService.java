package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.OAuthLoginRequest;
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
}