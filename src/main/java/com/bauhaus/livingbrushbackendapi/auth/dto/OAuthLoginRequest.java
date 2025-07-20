package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;

/**
 * 모든 OAuth 로그인 요청이 구현해야 할 공통 인터페이스입니다. (리팩토링 v2.0)
 *
 * abstract class 대신 interface로 변경하여, 구현체들이 Java record를
 * 자유롭게 사용할 수 있도록 하고 불변성을 보장합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
public interface OAuthLoginRequest {

    /**
     * 요청이 발생한 플랫폼(VR, AR 등) 정보를 반환합니다.
     *
     * @return Platform 열거형
     */
    Platform getPlatform();
}