package com.bauhaus.livingbrushbackendapi.storage.service;

import com.bauhaus.livingbrushbackendapi.config.AppProperties;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 로컬 파일 저장 서비스 구현체 (리팩토링 v2.2)
 *
 * - 예외 처리를 CustomException과 ErrorCode 시스템으로 통일하여 일관성을 확보합니다.
 */
@Slf4j
@Service("localFileStorageService")
@Profile("local")
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;
    private final String webPathPrefix;

    public LocalFileStorageService(AppProperties appProperties) {
        this.rootLocation = Paths.get(appProperties.getQr().getLocalPath()).toAbsolutePath().normalize();
        this.webPathPrefix = appProperties.getQr().getWebPath();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("로컬 파일 저장소 초기화 완료. 경로: {}", rootLocation);
        } catch (IOException e) {
            log.error("로컬 파일 저장소 디렉토리 생성 실패. 경로: {}", rootLocation, e);
            // 이제 이 코드는 CustomException의 새로운 생성자 덕분에 정상적으로 동작합니다.
            throw new CustomException(ErrorCode.DIRECTORY_CREATION_FAILED, e);
        }
    }

    @Override
    public String save(byte[] fileData, String fileName) {
        try {
            Path destinationFile = this.rootLocation.resolve(Paths.get(fileName)).normalize();

            if (!destinationFile.getParent().equals(this.rootLocation)) {
                throw new CustomException(ErrorCode.INVALID_FILE_PATH);
            }

            Files.write(destinationFile, fileData);

            return this.webPathPrefix + fileName;

        } catch (IOException e) {
            log.error("로컬 파일 저장 중 오류 발생. 파일명: {}", fileName, e);
            // 이제 이 코드는 CustomException의 새로운 생성자 덕분에 정상적으로 동작합니다.
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        }
    }

    @Override
    public String saveWithContext(byte[] fileData, String fileName, FileStorageContext context) {
        try {
            // 컨텍스트 기반 폴더 경로 생성
            String contextPath = generateContextPath(context);
            Path contextDir = this.rootLocation.resolve(contextPath);
            
            // 디렉토리 생성
            Files.createDirectories(contextDir);
            
            // 파일 저장
            Path destinationFile = contextDir.resolve(fileName);
            Files.write(destinationFile, fileData);
            
            // 웹 경로 생성
            String relativePath = this.rootLocation.relativize(destinationFile).toString().replace("\\", "/");
            return this.webPathPrefix + relativePath;

        } catch (IOException e) {
            log.error("로컬 파일 저장 중 오류 발생. 파일명: {}, 컨텍스트: {}", fileName, context, e);
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
            // URL에서 로컬 파일 경로 추출
            Path filePath = extractLocalPathFromUrl(fileUrl);
            
            log.debug("로컬 파일 삭제 시작 - 경로: {}, URL: {}", filePath, fileUrl);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("로컬 파일 삭제 완료 - 경로: {}, URL: {}", filePath, fileUrl);
            } else {
                log.warn("삭제하려는 로컬 파일이 존재하지 않음 - 경로: {}, URL: {}", filePath, fileUrl);
            }

        } catch (IOException e) {
            log.error("로컬 파일 삭제 중 오류 발생 - URL: {}, 오류: {}", fileUrl, e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        } catch (Exception e) {
            log.error("로컬 파일 삭제 중 예상치 못한 오류 발생 - URL: {}, 오류: {}", fileUrl, e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        }
    }

    /**
     * 컨텍스트 정보를 바탕으로 폴더 경로를 생성합니다.
     * Media 중심의 독립적 구조로 설계되었습니다.
     */
    private String generateContextPath(FileStorageContext context) {
        return switch (context.getFileType()) {
            case QR_CODE -> {
                // qr-codes/user-123/artwork-456/
                yield String.format("qr-codes/user-%d/artwork-%d", 
                        context.getUserId(), context.getArtworkId());
            }
            case ARTWORK_GLB -> {
                // artworks/user-123/artwork-456/
                yield String.format("artworks/user-%d/artwork-%d", 
                        context.getUserId(), context.getArtworkId());
            }
            case MEDIA -> {
                // media/user-123/media-456/ (독립적, artwork와 무관)
                yield String.format("media/user-%d/media-%d", 
                        context.getUserId(), context.getMediaId());
            }
            case PROFILE_IMAGE -> {
                // profiles/user-123/
                yield String.format("profiles/user-%d", context.getUserId());
            }
            case OTHER -> {
                // uploads/user-123/ (사용자별로만 분리)
                yield String.format("uploads/user-%d", context.getUserId());
            }
        };
    }

    /**
     * 웹 URL에서 로컬 파일 시스템 경로를 추출합니다.
     * 
     * @param fileUrl 웹 접근 URL
     * @return 로컬 파일 시스템 경로
     * @throws IllegalArgumentException URL 형식이 올바르지 않은 경우
     */
    private Path extractLocalPathFromUrl(String fileUrl) {
        try {
            if (!fileUrl.startsWith(this.webPathPrefix)) {
                throw new IllegalArgumentException("올바르지 않은 로컬 파일 URL 형식: " + fileUrl);
            }
            
            // 웹 경로 prefix 제거하여 상대 경로 추출
            String relativePath = fileUrl.substring(this.webPathPrefix.length());
            
            // 로컬 파일 시스템 경로로 변환
            Path filePath = this.rootLocation.resolve(relativePath).normalize();
            
            // 보안 검증: rootLocation 내부의 파일인지 확인
            if (!filePath.startsWith(this.rootLocation)) {
                throw new IllegalArgumentException("허용되지 않은 파일 경로: " + relativePath);
            }
            
            return filePath;
            
        } catch (Exception e) {
            log.error("로컬 URL에서 경로 추출 실패 - URL: {}, 오류: {}", fileUrl, e.getMessage());
            throw new IllegalArgumentException("로컬 URL 파싱 실패: " + fileUrl, e);
        }
    }
}