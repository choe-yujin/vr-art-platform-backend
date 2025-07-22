package com.bauhaus.livingbrushbackendapi.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.UUID;

/**
 * 작품 URL 보안 서비스
 * 
 * Long ID를 유지하면서도 웹XR URL을 안전하게 보호하는 서비스입니다.
 * 직접적인 순차 접근을 방지하고 인증된 경로를 통해서만 접근 가능하도록 합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Service
public class ArtworkUrlSecurityService {

    @Value("${app.artwork.secret-key:livingbrush-secret-2024}")
    private String secretKey;

    /**
     * 작품 ID와 사용자 정보를 기반으로 액세스 토큰을 생성합니다.
     * 
     * 이 토큰은 QR 코드나 정당한 링크를 통해서만 접근할 수 있도록 보장합니다.
     * 
     * @param artworkId 작품 ID
     * @param userId 소유자 사용자 ID
     * @return 액세스 토큰
     */
    public String generateAccessToken(Long artworkId, Long userId) {
        String input = String.format("%d_%d_%s_%d", 
                artworkId, userId, secretKey, System.currentTimeMillis() / 60000); // 1분 단위
        
        String token = DigestUtils.md5DigestAsHex(input.getBytes()).substring(0, 16);
        
        log.debug("작품 액세스 토큰 생성: artwork={}, token={}", artworkId, token);
        return token;
    }

    /**
     * 작품 액세스 토큰을 검증합니다.
     * 
     * @param artworkId 작품 ID
     * @param userId 소유자 사용자 ID
     * @param providedToken 제공된 토큰
     * @return 유효성 여부
     */
    public boolean validateAccessToken(Long artworkId, Long userId, String providedToken) {
        if (providedToken == null || providedToken.length() != 16) {
            return false;
        }

        // 현재 시간과 이전 시간(1분전)으로 토큰 생성해서 비교 (시간차 허용)
        long currentMinute = System.currentTimeMillis() / 60000;
        
        for (int i = 0; i < 2; i++) { // 현재 + 1분 전까지 허용
            String input = String.format("%d_%d_%s_%d", 
                    artworkId, userId, secretKey, currentMinute - i);
            String expectedToken = DigestUtils.md5DigestAsHex(input.getBytes()).substring(0, 16);
            
            if (expectedToken.equals(providedToken)) {
                return true;
            }
        }

        log.warn("유효하지 않은 작품 액세스 토큰: artwork={}, token={}", artworkId, providedToken);
        return false;
    }

    /**
     * 공개 작품용 영구 액세스 토큰 생성
     * 
     * 공개 작품의 경우 시간에 무관한 영구 토큰을 생성합니다.
     * 
     * @param artworkId 작품 ID
     * @return 영구 액세스 토큰
     */
    public String generatePublicAccessToken(Long artworkId) {
        String input = String.format("public_%d_%s", artworkId, secretKey);
        String token = DigestUtils.md5DigestAsHex(input.getBytes()).substring(0, 16);
        
        log.debug("공개 작품 영구 토큰 생성: artwork={}, token={}", artworkId, token);
        return token;
    }

    /**
     * 공개 작품 액세스 토큰을 검증합니다.
     * 
     * @param artworkId 작품 ID
     * @param providedToken 제공된 토큰
     * @return 유효성 여부
     */
    public boolean validatePublicAccessToken(Long artworkId, String providedToken) {
        if (providedToken == null || providedToken.length() != 16) {
            return false;
        }

        String expectedToken = generatePublicAccessToken(artworkId);
        boolean isValid = expectedToken.equals(providedToken);
        
        if (!isValid) {
            log.warn("유효하지 않은 공개 작품 액세스 토큰: artwork={}, token={}", artworkId, providedToken);
        }
        
        return isValid;
    }

    /**
     * 안전한 WebXR URL 생성
     * 
     * 작품의 공개/비공개 상태에 따라 적절한 액세스 토큰을 포함한 URL을 생성합니다.
     * 
     * @param artworkId 작품 ID
     * @param userId 소유자 사용자 ID
     * @param isPublic 공개 여부
     * @param baseUrl WebXR 기본 URL
     * @return 보안 토큰이 포함된 WebXR URL
     */
    public String generateSecureWebXrUrl(Long artworkId, Long userId, boolean isPublic, String baseUrl) {
        String token = isPublic ? 
                generatePublicAccessToken(artworkId) : 
                generateAccessToken(artworkId, userId);
        
        String secureUrl = String.format("%s/ar/view/%d?token=%s", baseUrl, artworkId, token);
        
        log.debug("보안 WebXR URL 생성: artwork={}, url={}", artworkId, secureUrl);
        return secureUrl;
    }

    /**
     * QR 코드 스캔용 일회성 토큰 생성
     * 
     * QR 코드를 통한 접근 시 사용되는 일회성 토큰을 생성합니다.
     * 
     * @param artworkId 작품 ID
     * @return 일회성 토큰
     */
    public String generateQrScanToken(Long artworkId) {
        String randomComponent = UUID.randomUUID().toString().substring(0, 8);
        String input = String.format("qr_%d_%s_%s", artworkId, secretKey, randomComponent);
        String token = DigestUtils.md5DigestAsHex(input.getBytes());
        
        log.debug("QR 스캔 토큰 생성: artwork={}, token={}", artworkId, token);
        return token;
    }

    /**
     * 작품 ID를 난독화된 형태로 인코딩
     * 
     * 직접적인 숫자 ID 노출을 피하기 위해 인코딩합니다.
     * 
     * @param artworkId 작품 ID
     * @return 인코딩된 ID
     */
    public String encodeArtworkId(Long artworkId) {
        // 간단한 XOR 인코딩 + Base36 변환
        long encoded = artworkId ^ 0xABCDEF123456L;
        return Long.toString(encoded, 36).toUpperCase();
    }

    /**
     * 인코딩된 작품 ID를 디코딩
     * 
     * @param encodedId 인코딩된 ID
     * @return 원본 작품 ID
     */
    public Long decodeArtworkId(String encodedId) {
        try {
            long encoded = Long.parseLong(encodedId, 36);
            return encoded ^ 0xABCDEF123456L;
        } catch (NumberFormatException e) {
            log.warn("유효하지 않은 인코딩된 작품 ID: {}", encodedId);
            return null;
        }
    }

    /**
     * 보안 수준별 URL 생성 정책
     */
    public enum SecurityLevel {
        /**
         * 최고 보안: 시간 제한 토큰 + 인코딩된 ID
         * 사용 사례: 비공개 작품, 프리미엄 콘텐츠
         */
        MAXIMUM,
        
        /**
         * 표준 보안: 영구 토큰 + 실제 ID
         * 사용 사례: 공개 작품, 일반 공유
         */
        STANDARD,
        
        /**
         * 최소 보안: 토큰 없이 직접 접근 허용
         * 사용 사례: 완전 공개 콘텐츠, 데모용
         */
        MINIMAL
    }

    /**
     * 보안 수준에 따른 WebXR URL 생성
     * 
     * @param artworkId 작품 ID
     * @param userId 소유자 사용자 ID
     * @param securityLevel 보안 수준
     * @param baseUrl WebXR 기본 URL
     * @return 보안 정책이 적용된 URL
     */
    public String generateUrlBySecurityLevel(Long artworkId, Long userId, SecurityLevel securityLevel, String baseUrl) {
        return switch (securityLevel) {
            case MAXIMUM -> {
                String encodedId = encodeArtworkId(artworkId);
                String token = generateAccessToken(artworkId, userId);
                yield String.format("%s/ar/view/%s?token=%s", baseUrl, encodedId, token);
            }
            case STANDARD -> {
                String token = generatePublicAccessToken(artworkId);
                yield String.format("%s/ar/view/%d?token=%s", baseUrl, artworkId, token);
            }
            case MINIMAL -> String.format("%s/ar/view/%d", baseUrl, artworkId);
        };
    }
}
