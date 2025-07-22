package com.bauhaus.livingbrushbackendapi.storage.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 저장 컨텍스트 정보
 * 
 * 파일을 저장할 때 필요한 비즈니스 컨텍스트 정보를 담는 클래스입니다.
 * Media 도메인의 독립성을 고려하여 설계되었습니다.
 * 
 * @author Bauhaus Team
 * @since 2.0
 */
@Getter
@RequiredArgsConstructor
@Builder
public class FileStorageContext {
    
    /**
     * 파일 유형
     */
    public enum FileType {
        // 작품 종속 파일들 (항상 artworkId 필요)
        QR_CODE,        // QR 코드 이미지
        ARTWORK_GLB,    // VR에서 생성한 .glb 원본 파일
        
        // 독립적 파일들
        MEDIA,          // 미디어 파일 (나중에 작품과 연결될 수 있음)
        PROFILE_IMAGE,  // 프로필 이미지
        PAIRING_QR,     // 계정 페어링용 QR 코드
        
        // 기타
        OTHER
    }
    
    /**
     * 파일 유형
     */
    private final FileType fileType;
    
    /**
     * 사용자 ID (필수)
     */
    private final Long userId;
    
    /**
     * 작품 ID (QR_CODE, ARTWORK_GLB 타입일 때 필수)
     * UUID 문자열 또는 Long 타입 모두 지원
     */
    private final String artworkId;
    
    /**
     * 미디어 ID (MEDIA 타입일 때 필수)
     */
    private final Long mediaId;
    
    /**
     * 추가 경로 정보 (선택사항)
     */
    private final String additionalPath;
    
    /**
     * QR 코드용 팩토리 메서드
     */
    public static FileStorageContext forQrCode(Long userId, String artworkId) {
        return FileStorageContext.builder()
                .fileType(FileType.QR_CODE)
                .userId(userId)
                .artworkId(artworkId)
                .build();
    }
    
    /**
     * QR 코드용 팩토리 메서드 (Long 타입 하위 호환성)
     */
    public static FileStorageContext forQrCode(Long userId, Long artworkId) {
        return forQrCode(userId, String.valueOf(artworkId));
    }
    
    /**
     * VR 작품 .glb 파일용 팩토리 메서드
     */
    public static FileStorageContext forArtworkGlb(Long userId, String artworkId) {
        return FileStorageContext.builder()
                .fileType(FileType.ARTWORK_GLB)
                .userId(userId)
                .artworkId(artworkId)
                .build();
    }
    
    /**
     * VR 작품 .glb 파일용 팩토리 메서드 (Long 타입 하위 호환성)
     */
    public static FileStorageContext forArtworkGlb(Long userId, Long artworkId) {
        return forArtworkGlb(userId, String.valueOf(artworkId));
    }
    
    /**
     * 미디어 파일용 팩토리 메서드 (독립적 저장)
     * 
     * @param userId 사용자 ID
     * @param mediaId 미디어 ID (DB에서 생성된 고유 ID)
     * @return FileStorageContext
     */
    public static FileStorageContext forMedia(Long userId, Long mediaId) {
        return FileStorageContext.builder()
                .fileType(FileType.MEDIA)
                .userId(userId)
                .mediaId(mediaId)
                .build();
    }
    
    /**
     * 프로필 이미지용 팩토리 메서드
     */
    public static FileStorageContext forProfileImage(Long userId) {
        return FileStorageContext.builder()
                .fileType(FileType.PROFILE_IMAGE)
                .userId(userId)
                .build();
    }
    
    /**
     * 계정 페어링 QR 코드용 팩토리 메서드
     */
    public static FileStorageContext forPairingQr(Long userId) {
        return FileStorageContext.builder()
                .fileType(FileType.PAIRING_QR)
                .userId(userId)
                .additionalPath("pairing")
                .build();
    }
}
