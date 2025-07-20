package com.bauhaus.livingbrushbackendapi.common.service;

import com.bauhaus.livingbrushbackendapi.config.AppProperties;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
}