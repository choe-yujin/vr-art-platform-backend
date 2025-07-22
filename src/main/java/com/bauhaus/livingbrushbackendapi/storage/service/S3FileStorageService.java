package com.bauhaus.livingbrushbackendapi.storage.service;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * AWS S3 파일 저장 서비스 구현체
 *
 * AWS S3에 파일을 업로드하고 퍼블릭 URL을 반환하는 FileStorageService 구현체입니다.
 * local 프로필이 아닌 환경(dev, prod)에서 활성화됩니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Service("s3FileStorageService")
@Profile({"dev", "prod", "local-s3"})
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Override
    public String save(byte[] fileData, String fileName) {
        try {
            // 파일 유형에 따라 폴더 경로 자동 생성 (기존 방식 - 하위 호환성)
            String s3Key = generateS3Key(fileName);
            
            log.debug("S3 파일 업로드 시작 - S3 키: {}, 크기: {} bytes", s3Key, fileData.length);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(determineContentType(fileName))
                    .contentLength((long) fileData.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));

            String publicUrl = generatePublicUrl(s3Key);
            log.info("S3 파일 업로드 완료 - S3 키: {}, URL: {}", s3Key, publicUrl);
            
            return publicUrl;

        } catch (SdkException e) {
            log.error("S3 파일 업로드 중 오류 발생 - 파일명: {}, 오류: {}", fileName, e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        }
    }

    @Override
    public String saveWithContext(byte[] fileData, String fileName, FileStorageContext context) {
        try {
            // 컨텍스트 기반 S3 키 생성
            String s3Key = generateContextBasedS3Key(fileName, context);
            
            log.debug("S3 컨텍스트 기반 파일 업로드 시작 - S3 키: {}, 크기: {} bytes", s3Key, fileData.length);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(determineContentType(fileName))
                    .contentLength((long) fileData.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));

            String publicUrl = generatePublicUrl(s3Key);
            log.info("S3 컨텍스트 기반 파일 업로드 완료 - S3 키: {}, URL: {}", s3Key, publicUrl);
            
            return publicUrl;

        } catch (SdkException e) {
            log.error("S3 컨텍스트 기반 파일 업로드 중 오류 발생 - 파일명: {}, 컨텍스트: {}, 오류: {}", 
                    fileName, context, e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            log.debug("삭제할 파일 URL이 비어있음 - 삭제 생략");
            return;
        }

        try {
            // URL에서 S3 키 추출
            String s3Key = extractS3KeyFromUrl(fileUrl);
            
            log.debug("S3 파일 삭제 시작 - S3 키: {}, URL: {}", s3Key, fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            
            log.info("S3 파일 삭제 완료 - S3 키: {}, URL: {}", s3Key, fileUrl);

        } catch (SdkException e) {
            log.error("S3 파일 삭제 중 오류 발생 - URL: {}, 오류: {}", fileUrl, e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        } catch (Exception e) {
            log.error("S3 파일 삭제 중 예상치 못한 오류 발생 - URL: {}, 오류: {}", fileUrl, e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        }
    }

    /**
     * 컨텍스트 정보를 바탕으로 S3 키를 생성합니다.
     * Media 중심의 독립적 구조로 설계되었습니다.
     */
    private String generateContextBasedS3Key(String fileName, FileStorageContext context) {
        return switch (context.getFileType()) {
            case QR_CODE -> {
                // qr-codes/user-123/artwork-456/filename.png
                yield String.format("qr-codes/user-%d/artwork-%d/%s", 
                        context.getUserId(), context.getArtworkId(), fileName);
            }
            case ARTWORK_GLB -> {
                // artworks/user-123/artwork-456/filename.glb
                yield String.format("artworks/user-%d/artwork-%d/%s", 
                        context.getUserId(), context.getArtworkId(), fileName);
            }
            case MEDIA -> {
                // media/user-123/media-456/filename.jpg (독립적, artwork와 무관)
                yield String.format("media/user-%d/media-%d/%s", 
                        context.getUserId(), context.getMediaId(), fileName);
            }
            case PROFILE_IMAGE -> {
                // profiles/user-123/filename.jpg
                yield String.format("profiles/user-%d/%s", context.getUserId(), fileName);
            }
            case PAIRING_QR -> {
                // pairing-qr/user-123/filename.png (사용자별 관리)
                yield String.format("pairing-qr/user-%d/%s", context.getUserId(), fileName);
            }
            case OTHER -> {
                // uploads/user-123/filename.ext
                yield String.format("uploads/user-%d/%s", context.getUserId(), fileName);
            }
        };
    }

    /**
     * 파일 이름과 유형에 따라 S3 키(경로)를 생성합니다.
     *
     * @param fileName 원본 파일명
     * @return S3 객체 키 (폴더 경로 포함)
     */
    private String generateS3Key(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        String datePrefix = java.time.LocalDate.now().toString(); // 2024-07-21
        
        if (lowerCaseFileName.endsWith(".png") && fileName.length() == 40) {
            // QR 코드로 추정 (UUID + .png = 36 + 4 = 40자)
            return String.format("qr-codes/%s/%s", datePrefix, fileName);
        } else if (lowerCaseFileName.endsWith(".glb")) {
            // VR 작품 파일
            return String.format("artworks/%s/%s", datePrefix, fileName);
        } else if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            // 썸네일 이미지
            return String.format("thumbnails/%s/%s", datePrefix, fileName);
        } else {
            // 기타 파일
            return String.format("uploads/%s/%s", datePrefix, fileName);
        }
    }

    /**
     * 파일 확장자를 기반으로 Content-Type을 결정합니다.
     *
     * @param fileName 파일명
     * @return MIME 타입
     */
    private String determineContentType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        
        if (lowerCaseFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerCaseFileName.endsWith(".glb")) {
            return "model/gltf-binary";
        } else if (lowerCaseFileName.endsWith(".gltf")) {
            return "model/gltf+json";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * S3 객체의 퍼블릭 URL을 생성합니다.
     *
     * @param s3Key S3 객체 키(폴더 경로 포함)
     * @return 퍼블릭 액세스 가능한 URL
     */
    private String generatePublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }

    /**
     * S3 퍼블릭 URL에서 S3 키를 추출합니다.
     * 
     * @param fileUrl S3 퍼블릭 URL
     * @return S3 객체 키
     * @throws IllegalArgumentException URL 형식이 올바르지 않은 경우
     */
    private String extractS3KeyFromUrl(String fileUrl) {
        try {
            // URL 형식: https://bucket-name.s3.region.amazonaws.com/s3-key
            String expectedPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            
            if (!fileUrl.startsWith(expectedPrefix)) {
                throw new IllegalArgumentException("올바르지 않은 S3 URL 형식: " + fileUrl);
            }
            
            return fileUrl.substring(expectedPrefix.length());
            
        } catch (Exception e) {
            log.error("S3 URL에서 키 추출 실패 - URL: {}, 오류: {}", fileUrl, e.getMessage());
            throw new IllegalArgumentException("S3 URL 파싱 실패: " + fileUrl, e);
        }
    }
}
