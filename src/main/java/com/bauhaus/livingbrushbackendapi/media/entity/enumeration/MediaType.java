package com.bauhaus.livingbrushbackendapi.media.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 미디어 타입 열거형 (V1 DB 스크립트 완벽 호환)
 *
 * V1 DB ENUM: media_type AS ENUM ('AUDIO', 'IMAGE', 'MODEL_3D', 'VIDEO')
 * Hibernate @Enumerated(EnumType.STRING)과 완벽 호환
 * 
 * 순수한 상수 정의만 포함 - 모든 비즈니스 로직은 Service 계층에서 처리
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum MediaType {
    
    /**
     * 오디오 파일 (V1: 'AUDIO')
     * - MP3, WAV, AAC 등
     * - 최대 600초 (10분)
     */
    AUDIO("AUDIO", "오디오", true, 600),
    
    /**
     * 이미지 파일 (V1: 'IMAGE')  
     * - PNG, JPG, JPEG, GIF 등
     * - duration_seconds는 NULL
     */
    IMAGE("IMAGE", "이미지", false, null),
    
    /**
     * 3D 모델 파일 (V1: 'MODEL_3D')
     * - GLB, GLTF 등
     * - duration_seconds는 NULL
     */
    MODEL_3D("MODEL_3D", "3D 모델", false, null),
    
    /**
     * 비디오 파일 (V1: 'VIDEO')
     * - MP4, AVI, MOV 등  
     * - 최대 600초 (10분)
     */
    VIDEO("VIDEO", "비디오", true, 600);

    /**
     * 미디어 타입 코드 (V1 DB ENUM 값과 완전 일치)
     */
    private final String code;
    
    /**
     * 미디어 타입 이름 (한글)
     */
    private final String displayName;
    
    /**
     * 재생 시간 제한이 있는지 여부 (메타데이터)
     */
    private final boolean hasDuration;
    
    /**
     * 최대 재생 시간 (초) (메타데이터)
     */
    private final Integer maxDurationSeconds;

    /**
     * 코드로 MediaType 찾기 (단순 조회만)
     */
    public static MediaType fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (MediaType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * JSON 직렬화용 문자열 반환 (V1 DB 값)
     */
    @Override
    public String toString() {
        return this.code;
    }
}
