package com.bauhaus.livingbrushbackendapi.user.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 이미지 업로드 응답 DTO
 * 
 * 프로필 이미지 업로드 API의 응답으로 사용됩니다.
 * 업로드된 이미지의 URL과 메타 정보를 제공합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileImageUploadResponse {

    /**
     * 업로드 성공 여부
     */
    private boolean success;

    /**
     * 업로드된 이미지 URL (S3 또는 CDN URL)
     */
    private String imageUrl;

    /**
     * 원본 파일명
     */
    private String originalFileName;

    /**
     * 파일 크기 (bytes)
     */
    private long fileSize;

    /**
     * 메시지 (성공/실패 상세 정보)
     */
    private String message;

    @Builder
    private ProfileImageUploadResponse(boolean success, String imageUrl, String originalFileName,
                                     long fileSize, String message) {
        this.success = success;
        this.imageUrl = imageUrl;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.message = message;
    }

    /**
     * 업로드 성공 응답을 생성합니다.
     * 
     * @param imageUrl 업로드된 이미지 URL
     * @param originalFileName 원본 파일명
     * @param fileSize 파일 크기
     * @return 성공 응답 객체
     */
    public static ProfileImageUploadResponse success(String imageUrl, String originalFileName, long fileSize) {
        return ProfileImageUploadResponse.builder()
                .success(true)
                .imageUrl(imageUrl)
                .originalFileName(originalFileName)
                .fileSize(fileSize)
                .message("프로필 이미지 업로드가 완료되었습니다")
                .build();
    }

    /**
     * 업로드 실패 응답을 생성합니다.
     * 
     * @param message 실패 사유
     * @return 실패 응답 객체
     */
    public static ProfileImageUploadResponse failure(String message) {
        return ProfileImageUploadResponse.builder()
                .success(false)
                .imageUrl(null)
                .originalFileName(null)
                .fileSize(0)
                .message(message)
                .build();
    }
}
