package com.bauhaus.livingbrushbackendapi.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 작품 ID 생성 서비스
 * 
 * 웹XR에서 직접 접근 가능한 URL을 고려하여 
 * 예측 불가능하고 안전한 작품 ID를 생성합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Service
public class ArtworkIdGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * UUID 기반 작품 ID 생성 (권장)
     * 
     * 완전히 예측 불가능하고 충돌 확률이 극히 낮은 ID를 생성합니다.
     * 웹XR URL에서 직접 노출되어도 안전합니다.
     * 
     * 예시: a1b2c3d4-e5f6-7890-abcd-ef1234567890
     * 
     * @return UUID 문자열
     */
    public String generateUuidBasedId() {
        String artworkId = UUID.randomUUID().toString();
        log.debug("UUID 기반 작품 ID 생성: {}", artworkId);
        return artworkId;
    }

    /**
     * 해시 기반 작품 ID 생성 (SEO 친화적)
     * 
     * 작품 제목, 사용자 ID, 타임스탬프를 조합하여 
     * 16자리 해시 ID를 생성합니다.
     * 
     * 예시: a1b2c3d4e5f67890
     * 
     * @param title 작품 제목
     * @param userId 사용자 ID
     * @return 16자리 해시 ID
     */
    public String generateHashBasedId(String title, Long userId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String input = String.format("%s_%d_%s_%d", 
                title, userId, timestamp, System.nanoTime());
        
        String hash = DigestUtils.md5DigestAsHex(input.getBytes());
        String artworkId = hash.substring(0, 16); // 16자리로 단축
        
        log.debug("해시 기반 작품 ID 생성: {} -> {}", input, artworkId);
        return artworkId;
    }

    /**
     * 짧은 UUID 기반 작품 ID 생성 (URL 친화적)
     * 
     * UUID의 하이픈을 제거하고 첫 16자리만 사용하여
     * 상대적으로 짧은 ID를 생성합니다.
     * 
     * 예시: a1b2c3d4e5f67890
     * 
     * @return 16자리 UUID (하이픈 제거)
     */
    public String generateShortUuidId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String artworkId = uuid.substring(0, 16);
        
        log.debug("짧은 UUID 기반 작품 ID 생성: {}", artworkId);
        return artworkId;
    }

    /**
     * 커스텀 접두사가 있는 작품 ID 생성
     * 
     * 브랜드 식별이나 카테고리 구분을 위해 
     * 접두사를 포함한 ID를 생성합니다.
     * 
     * 예시: lbr_a1b2c3d4e5f67890
     * 
     * @param prefix 접두사 (예: "lbr", "art")
     * @return 접두사가 포함된 작품 ID
     */
    public String generatePrefixedId(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String shortId = uuid.substring(0, 12);
        String artworkId = String.format("%s_%s", prefix, shortId);
        
        log.debug("접두사 포함 작품 ID 생성: {}", artworkId);
        return artworkId;
    }

    /**
     * 타임스탬프 기반 작품 ID 생성 (개발/테스트용)
     * 
     * 개발 환경에서 순차적 확인이 필요한 경우 사용합니다.
     * 운영 환경에서는 사용하지 않는 것을 권장합니다.
     * 
     * 예시: 20240722143052_a1b2c3d4
     * 
     * @param userId 사용자 ID
     * @return 타임스탬프 기반 ID
     */
    public String generateTimestampBasedId(Long userId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        String artworkId = String.format("%s_%d_%s", timestamp, userId, randomSuffix);
        
        log.debug("타임스탬프 기반 작품 ID 생성: {}", artworkId);
        return artworkId;
    }

    /**
     * 작품 ID 유효성 검증
     * 
     * 생성된 작품 ID가 유효한 형식인지 확인합니다.
     * 
     * @param artworkId 검증할 작품 ID
     * @return 유효성 여부
     */
    public boolean isValidArtworkId(String artworkId) {
        if (artworkId == null || artworkId.trim().isEmpty()) {
            return false;
        }

        // UUID 형식 확인 (하이픈 포함)
        if (artworkId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            return true;
        }

        // 16자리 해시 형식 확인
        if (artworkId.matches("^[0-9a-f]{16}$")) {
            return true;
        }

        // 접두사 포함 형식 확인
        if (artworkId.matches("^[a-z]+_[0-9a-f]{12,16}$")) {
            return true;
        }

        log.warn("유효하지 않은 작품 ID 형식: {}", artworkId);
        return false;
    }

    /**
     * 작품 ID에서 접두사 추출
     * 
     * @param artworkId 작품 ID
     * @return 접두사 (없으면 null)
     */
    public String extractPrefix(String artworkId) {
        if (artworkId != null && artworkId.contains("_")) {
            return artworkId.substring(0, artworkId.indexOf("_"));
        }
        return null;
    }
}
