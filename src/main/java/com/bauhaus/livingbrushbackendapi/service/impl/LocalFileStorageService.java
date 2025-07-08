package com.bauhaus.livingbrushbackendapi.service.impl;

import com.bauhaus.livingbrushbackendapi.config.AppProperties;
import com.bauhaus.livingbrushbackendapi.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service("localFileStorageService") // Bean 이름을 명시하여 다른 구현체와 구분
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final AppProperties appProperties;

    @Override
    public String save(byte[] fileData, String fileName) {
        try {
            // 설정 파일에서 QR 이미지를 저장할 기본 경로를 가져옵니다.
            Path directoryPath = Paths.get(appProperties.getQr().getLocalPath());

            // 디렉토리가 존재하지 않으면 생성합니다.
            Files.createDirectories(directoryPath);

            // 최종 파일 경로를 조합합니다.
            Path filePath = directoryPath.resolve(fileName);

            // 파일 데이터를 디스크에 씁니다.
            Files.write(filePath, fileData);

            // 정적 파일 제공 경로를 기반으로 웹 접근 URL을 생성하여 반환합니다.
            // 예: /qr-images/some-uuid.png
            return "/qr-images/" + fileName;

        } catch (IOException e) {
            log.error("로컬 파일 저장 중 오류 발생. 파일명: {}", fileName, e);
            // 실제 프로덕션 코드에서는 더 구체적인 예외를 던지는 것이 좋습니다.
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }
}