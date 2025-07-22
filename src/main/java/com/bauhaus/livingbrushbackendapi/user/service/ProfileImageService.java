package com.bauhaus.livingbrushbackendapi.user.service;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageContext;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * 프로필 이미지 전용 업로드 서비스
 * 
 * OAuth 프로필 이미지를 다운로드하여 S3에 업로드하는 서비스입니다.
 * 인터페이스 패턴을 사용하여 Local/S3 저장소를 자동으로 선택합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final FileStorageService fileStorageService;
    private final WebClient.Builder webClientBuilder;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket:livingbrush-storage}")
    private String bucketName;

    @Value("${aws.cloudfront.domain:livingbrush-storage.s3.ap-northeast-2.amazonaws.com}")
    private String cloudFrontDomain;

    @Value("${app.profile.default-image-url:https://livingbrush-storage.s3.ap-northeast-2.amazonaws.com/profile/default-avatar.png}")
    private String defaultProfileImageUrl;

    /**
     * OAuth 프로필 이미지를 다운로드하여 S3에 업로드합니다.
     * 
     * @param userId 사용자 ID
     * @param oauthImageUrl OAuth에서 제공받은 이미지 URL
     * @return S3에 업로드된 이미지의 퍼블릭 URL
     */
    public String uploadProfileImageFromUrl(Long userId, String oauthImageUrl) {
        if (oauthImageUrl == null || oauthImageUrl.isBlank()) {
            log.debug("OAuth 이미지 URL이 비어있음 - 기본 이미지 URL 반환. 사용자 ID: {}", userId);
            return defaultProfileImageUrl;
        }

        try {
            log.debug("OAuth 프로필 이미지 다운로드 시작 - 사용자 ID: {}, URL: {}", userId, oauthImageUrl);

            // OAuth 이미지 다운로드
            byte[] imageData = downloadImageFromUrl(oauthImageUrl);
            
            if (imageData.length == 0) {
                log.warn("다운로드된 이미지 데이터가 비어있음 - 기본 이미지 사용. 사용자 ID: {}", userId);
                return defaultProfileImageUrl;
            }

            // 고유한 파일명 생성 (UUID + 확장자)
            String fileName = generateProfileImageFileName(oauthImageUrl);
            
            // 파일 저장 컨텍스트 생성
            FileStorageContext context = FileStorageContext.builder()
                    .userId(userId)
                    .fileType(FileStorageContext.FileType.PROFILE_IMAGE)
                    .build();

            // S3 (또는 로컬) 저장소에 업로드
            String uploadedUrl = fileStorageService.saveWithContext(imageData, fileName, context);
            
            log.info("OAuth 프로필 이미지 업로드 완료 - 사용자 ID: {}, 원본 URL: {}, 저장된 URL: {}", 
                    userId, oauthImageUrl, uploadedUrl);
            
            return uploadedUrl;

        } catch (Exception e) {
            log.error("OAuth 프로필 이미지 업로드 실패 - 사용자 ID: {}, URL: {}, 오류: {}", 
                    userId, oauthImageUrl, e.getMessage(), e);
            
            // 업로드 실패 시 기본 이미지 URL 반환 (서비스 중단 방지)
            return defaultProfileImageUrl;
        }
    }

    /**
     * 사용자가 직접 업로드한 프로필 이미지를 S3에 저장합니다.
     * 
     * @param userId 사용자 ID
     * @param imageData 이미지 바이트 데이터
     * @param originalFileName 원본 파일명
     * @return S3에 업로드된 이미지의 퍼블릭 URL
     */
    public String uploadProfileImage(Long userId, byte[] imageData, String originalFileName) {
        try {
            if (imageData == null || imageData.length == 0) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }

            // 파일 크기 검증 (5MB 제한)
            if (imageData.length > 5 * 1024 * 1024) {
                throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
            }

            // 고유한 파일명 생성
            String fileName = generateProfileImageFileName(originalFileName);
            
            // 파일 저장 컨텍스트 생성
            FileStorageContext context = FileStorageContext.builder()
                    .userId(userId)
                    .fileType(FileStorageContext.FileType.PROFILE_IMAGE)
                    .build();

            // 저장소에 업로드
            String uploadedUrl = fileStorageService.saveWithContext(imageData, fileName, context);
            
            log.info("사용자 프로필 이미지 업로드 완료 - 사용자 ID: {}, 파일명: {}, URL: {}", 
                    userId, fileName, uploadedUrl);
            
            return uploadedUrl;

        } catch (CustomException e) {
            throw e; // CustomException은 그대로 전파
        } catch (Exception e) {
            log.error("사용자 프로필 이미지 업로드 실패 - 사용자 ID: {}, 오류: {}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        }
    }

    /**
     * URL에서 이미지를 다운로드합니다.
     * 
     * @param imageUrl 다운로드할 이미지 URL
     * @return 이미지 바이트 데이터
     * @throws IOException 다운로드 실패 시
     */
    private byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        WebClient webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB 제한
                .build();

        try {
            byte[] imageData = webClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(10)) // 10초 타임아웃
                    .block();

            if (imageData == null) {
                throw new IOException("다운로드된 이미지 데이터가 null입니다.");
            }

            log.debug("이미지 다운로드 완료 - URL: {}, 크기: {} bytes", imageUrl, imageData.length);
            return imageData;

        } catch (Exception e) {
            log.error("이미지 다운로드 실패 - URL: {}, 오류: {}", imageUrl, e.getMessage());
            throw new IOException("이미지 다운로드 실패: " + imageUrl, e);
        }
    }

    /**
     * 프로필 이미지용 고유한 파일명을 생성합니다.
     * 
     * @param originalUrl 원본 URL 또는 파일명
     * @return UUID 기반 고유 파일명
     */
    private String generateProfileImageFileName(String originalUrl) {
        // 확장자 추출 (기본값: .jpg)
        String extension = extractExtensionFromUrl(originalUrl);
        
        // UUID + 확장자로 고유 파일명 생성
        return String.format("profile_%s%s", UUID.randomUUID().toString().replace("-", ""), extension);
    }

    /**
     * URL 또는 파일명에서 확장자를 추출합니다.
     * 
     * @param url URL 또는 파일명
     * @return 확장자 (.jpg, .png 등)
     */
    private String extractExtensionFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return ".jpg"; // 기본 확장자
        }

        // URL에서 쿼리 파라미터 제거
        String cleanUrl = url.split("\\?")[0];
        
        // 확장자 추출
        int lastDotIndex = cleanUrl.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < cleanUrl.length() - 1) {
            String extension = cleanUrl.substring(lastDotIndex).toLowerCase();
            
            // 지원하는 이미지 확장자인지 확인
            if (extension.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                return extension;
            }
        }
        
        return ".jpg"; // 기본 확장자
    }

    /**
     * S3에서 프로필 이미지 파일을 삭제합니다.
     * 
     * @param imageUrl 삭제할 이미지의 S3 URL
     * @return 삭제 성공 여부
     */
    public boolean deleteProfileImageFromS3(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            log.debug("삭제할 이미지 URL이 비어있음");
            return false;
        }

        // 기본 이미지는 삭제하지 않음
        if (isDefaultProfileImage(imageUrl)) {
            log.debug("기본 프로필 이미지는 삭제하지 않음: {}", imageUrl);
            return false;
        }

        try {
            String s3Key = extractS3KeyFromUrl(imageUrl);
            if (s3Key == null) {
                log.warn("S3 키 추출 실패 - URL: {}", imageUrl);
                return false;
            }

            // S3에서 파일 삭제
            deleteFromS3(s3Key);
            
            log.info("S3 프로필 이미지 삭제 완료 - URL: {}, S3 Key: {}", imageUrl, s3Key);
            return true;
            
        } catch (Exception e) {
            log.error("S3 프로필 이미지 삭제 실패 - URL: {}, 오류: {}", imageUrl, e.getMessage());
            return false;
        }
    }

    /**
     * 기존 프로필 이미지를 새 이미지로 교체합니다.
     * 기존 이미지는 자동으로 S3에서 삭제됩니다.
     * 
     * @param userId 사용자 ID
     * @param imageData 새 이미지 데이터
     * @param originalFileName 원본 파일명
     * @param currentImageUrl 현재 프로필 이미지 URL (삭제 대상)
     * @return 새로 업로드된 이미지 URL
     */
    public String replaceProfileImage(Long userId, byte[] imageData, String originalFileName, String currentImageUrl) {
        try {
            // 1. 새 이미지 업로드
            String newImageUrl = uploadProfileImage(userId, imageData, originalFileName);
            
            // 2. 업로드 성공 시 기존 이미지 삭제 (비동기적으로 처리)
            if (currentImageUrl != null && !currentImageUrl.equals(newImageUrl)) {
                // 삭제 실패해도 업로드는 성공으로 처리
                boolean deleted = deleteProfileImageFromS3(currentImageUrl);
                if (deleted) {
                    log.info("기존 프로필 이미지 삭제 완료 - 사용자 ID: {}, 기존 URL: {}", userId, currentImageUrl);
                } else {
                    log.warn("기존 프로필 이미지 삭제 실패 (업로드는 성공) - 사용자 ID: {}, 기존 URL: {}", userId, currentImageUrl);
                }
            }
            
            return newImageUrl;
            
        } catch (Exception e) {
            log.error("프로필 이미지 교체 실패 - 사용자 ID: {}, 오류: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    // ========== 내부 헬퍼 메서드들 ==========

    /**
     * S3 URL에서 버킷 키를 추출합니다.
     */
    private String extractS3KeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            // CloudFront URL 패턴: https://domain.com/path/to/file.jpg
            // S3 직접 URL 패턴: https://bucket-name.s3.region.amazonaws.com/path/to/file.jpg
            
            String bucketName = getBucketName();
            String cloudFrontDomain = getCloudFrontDomain();
            
            // CloudFront URL인 경우
            if (imageUrl.contains(cloudFrontDomain)) {
                String path = imageUrl.substring(imageUrl.indexOf(cloudFrontDomain) + cloudFrontDomain.length());
                return path.startsWith("/") ? path.substring(1) : path;
            }
            
            // S3 직접 URL인 경우
            if (imageUrl.contains(bucketName + ".s3.")) {
                String[] parts = imageUrl.split("/");
                if (parts.length >= 4) {
                    // https://bucket.s3.region.amazonaws.com/path/to/file.jpg
                    // -> path/to/file.jpg
                    StringBuilder keyBuilder = new StringBuilder();
                    for (int i = 3; i < parts.length; i++) {
                        if (keyBuilder.length() > 0) {
                            keyBuilder.append("/");
                        }
                        keyBuilder.append(parts[i]);
                    }
                    return keyBuilder.toString();
                }
            }
            
            log.warn("S3 키 추출 실패 - 지원하지 않는 URL 형식: {}", imageUrl);
            return null;
            
        } catch (Exception e) {
            log.error("S3 키 추출 중 오류 발생 - URL: {}, 오류: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * S3에서 파일을 삭제합니다.
     */
    private void deleteFromS3(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            
            log.debug("S3 파일 삭제 성공 - bucket: {}, key: {}", getBucketName(), s3Key);
            
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패 - bucket: {}, key: {}, 오류: {}", 
                    getBucketName(), s3Key, e.getMessage());
            throw e;
        }
    }

    /**
     * 기본 프로필 이미지인지 확인합니다.
     */
    private boolean isDefaultProfileImage(String imageUrl) {
        return imageUrl != null && imageUrl.contains("default-avatar");
    }

    /**
     * 설정에서 S3 버킷명을 가져옵니다.
     */
    private String getBucketName() {
        return bucketName; // application.yml의 cloud.aws.s3.bucket 값
    }

    /**
     * 설정에서 CloudFront 도메인을 가져옵니다.
     */
    private String getCloudFrontDomain() {
        return cloudFrontDomain; // application.yml의 aws.cloudfront.domain 값
    }

    /**
     * 기본 프로필 이미지 URL을 반환합니다.
     * 
     * @return 기본 프로필 이미지 URL
     */
    public String getDefaultProfileImageUrl() {
        return defaultProfileImageUrl;
    }
}
