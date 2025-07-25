package com.bauhaus.livingbrushbackendapi.auth.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AR 앱에서 VR 로그인용 QR 코드 생성 요청 DTO
 * 
 * 현재는 추가 파라미터가 없지만, 향후 확장을 위해 구조를 유지합니다.
 * 실제 사용자 ID는 JWT 토큰에서 추출되므로 요청 본문에는 포함되지 않습니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VrLoginQrRequest {
    
    // 현재는 빈 요청이지만, 향후 확장 가능성을 위해 구조 유지
    // 예: QR 유효 시간 커스터마이징, 플랫폼 정보 등
    
    /**
     * 빈 VR QR 요청 객체를 생성합니다.
     * 
     * @return VR QR 요청 객체
     */
    public static VrLoginQrRequest of() {
        return new VrLoginQrRequest();
    }
}