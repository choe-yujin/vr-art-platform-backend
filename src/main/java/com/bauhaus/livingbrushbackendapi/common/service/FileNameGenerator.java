package com.bauhaus.livingbrushbackendapi.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 파일명 생성 서비스
 * 
 * S3에 저장될 파일의 고유하고 안전한 파일명을 생성합니다.
 * 한글, 특수문자, 공백 등을 처리하여 웹 안전한 파일명을 제공합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Service
public class FileNameGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final int MAX_FILENAME_LENGTH = 100; // S3 권장 최대 길이

    /**
     * VR 작품 GLB 파일을 위한 고유 파일명 생성
     * 
     * 형식: artwork_{timestamp}_{userId}_{shortUuid}.glb
     * 예시: artwork_20240722_143052_123_a1b2c3d4.glb
     * 
     * @param originalFileName 원본 파일명 (확장자 추출용)
     * @param userId 사용자 ID
     * @param artworkId 작품 ID (UUID 또는 해시 기반)
     * @return 생성된 고유 파일명
     */
    public String generateArtworkFileName(String originalFileName, Long userId, String artworkId) {
        String extension = extractExtension(originalFileName);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String shortUuid = generateShortUuid();
        
        String fileName = String.format("artwork_%s_%d_%s_%s.%s", 
                timestamp, userId, artworkId, shortUuid, extension);
        
        log.debug("작품 파일명 생성: {} -> {}", originalFileName, fileName);
        return fileName;
    }

    /**
     * 미디어 파일을 위한 고유 파일명 생성
     * 
     * 형식: media_{timestamp}_{userId}_{mediaId}_{shortUuid}.{ext}
     * 예시: media_20240722_143052_123_456_b2c3d4e5.jpg
     * 
     * @param originalFileName 원본 파일명
     * @param userId 사용자 ID
     * @param mediaId 미디어 ID
     * @return 생성된 고유 파일명
     */
    public String generateMediaFileName(String originalFileName, Long userId, Long mediaId) {
        String extension = extractExtension(originalFileName);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String shortUuid = generateShortUuid();
        
        String fileName = String.format("media_%s_%d_%d_%s.%s", 
                timestamp, userId, mediaId, shortUuid, extension);
        
        log.debug("미디어 파일명 생성: {} -> {}", originalFileName, fileName);
        return fileName;
    }

    /**
     * QR 코드 이미지를 위한 고유 파일명 생성
     * 
     * 형식: qr_{timestamp}_{userId}_{artworkId}_{shortUuid}.png
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @return 생성된 QR 파일명
     */
    public String generateQrFileName(Long userId, String artworkId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String shortUuid = generateShortUuid();
        
        String fileName = String.format("qr_%s_%d_%s_%s.png", 
                timestamp, userId, artworkId, shortUuid);
        
        log.debug("QR 파일명 생성: {}", fileName);
        return fileName;
    }

    /**
     * 프로필 이미지를 위한 고유 파일명 생성
     * 
     * 형식: profile_{timestamp}_{userId}_{shortUuid}.{ext}
     * 
     * @param originalFileName 원본 파일명
     * @param userId 사용자 ID
     * @return 생성된 프로필 이미지 파일명
     */
    public String generateProfileFileName(String originalFileName, Long userId) {
        String extension = extractExtension(originalFileName);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String shortUuid = generateShortUuid();
        
        String fileName = String.format("profile_%s_%d_%s.%s", 
                timestamp, userId, shortUuid, extension);
        
        log.debug("프로필 파일명 생성: {} -> {}", originalFileName, fileName);
        return fileName;
    }

    /**
     * 계정 페어링 QR 코드를 위한 고유 파일명 생성
     * 
     * 형식: pairing_{timestamp}_{userId}_{shortUuid}.png
     * 
     * @param userId 사용자 ID
     * @return 생성된 페어링 QR 파일명
     */
    public String generatePairingQrFileName(Long userId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String shortUuid = generateShortUuid();
        
        String fileName = String.format("pairing_%s_%d_%s.png", 
                timestamp, userId, shortUuid);
        
        log.debug("페어링 QR 파일명 생성: {}", fileName);
        return fileName;
    }

    /**
     * 원본 파일명을 웹 안전한 형태로 정규화
     * 
     * - 한글, 특수문자 제거
     * - 공백을 언더스코어로 변환
     * - 길이 제한 적용
     * 
     * @param originalFileName 원본 파일명
     * @return 정규화된 파일명
     */
    public String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "unnamed_file";
        }

        // 확장자 분리
        String nameWithoutExt = getNameWithoutExtension(originalFileName);
        String extension = extractExtension(originalFileName);

        // 안전하지 않은 문자 제거 및 공백 처리
        String sanitized = UNSAFE_CHARS.matcher(nameWithoutExt)
                .replaceAll("_")
                .replaceAll("_+", "_") // 연속된 언더스코어 정리
                .toLowerCase();

        // 길이 제한 적용
        if (sanitized.length() > MAX_FILENAME_LENGTH - extension.length() - 1) {
            sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH - extension.length() - 1);
        }

        // 언더스코어로 시작하거나 끝나는 경우 정리
        sanitized = sanitized.replaceAll("^_+|_+$", "");
        
        if (sanitized.isEmpty()) {
            sanitized = "file";
        }

        String result = sanitized + "." + extension;
        log.debug("파일명 정규화: {} -> {}", originalFileName, result);
        
        return result;
    }

    /**
     * 파일 확장자 추출 (소문자)
     * 
     * @param fileName 파일명
     * @return 확장자 (점 제외)
     */
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin"; // 기본 확장자
        }
        
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 확장자를 제외한 파일명 반환
     * 
     * @param fileName 파일명
     * @return 확장자를 제외한 파일명
     */
    private String getNameWithoutExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return fileName;
        }
        
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * 짧은 UUID 생성 (8자리)
     * 
     * 파일명이 너무 길어지지 않도록 UUID의 첫 8자리만 사용
     * 
     * @return 8자리 UUID
     */
    private String generateShortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 타임스탬프 기반 고유 ID 생성
     * 
     * 동시 업로드 시 충돌을 방지하기 위해 나노초 포함
     * 
     * @return 타임스탬프 기반 고유 ID
     */
    private String generateTimestampId() {
        return String.valueOf(System.nanoTime());
    }

    /**
     * 파일 타입별 접두사 반환
     * 
     * @param fileType 파일 타입
     * @return 접두사
     */
    private String getFileTypePrefix(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "glb", "gltf" -> "artwork";
            case "jpg", "jpeg", "png", "gif", "webp" -> "image";
            case "mp4", "mov", "avi", "webm" -> "video";
            case "mp3", "wav", "aac", "ogg" -> "audio";
            default -> "file";
        };
    }
}
