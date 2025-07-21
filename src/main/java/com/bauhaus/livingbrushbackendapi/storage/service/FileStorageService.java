package com.bauhaus.livingbrushbackendapi.storage.service;

/**
 * 파일 저장 서비스 인터페이스
 * 
 * 파일 저장 로직을 추상화하는 서비스 인터페이스입니다.
 * 구현체는 로컬 파일 시스템, 클라우드 스토리지(S3) 등 다양할 수 있습니다.
 * 
 * 구현체:
 * - LocalFileStorageService: 로컬 파일 시스템 저장 (개발용)
 * - S3FileStorageService: AWS S3 저장 (운영용)
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
public interface FileStorageService {

    /**
     * 파일 데이터를 저장소에 저장합니다.
     *
     * @param fileData 저장할 파일의 바이트 데이터
     * @param fileName 저장할 파일의 이름 (확장자 포함)
     * @return 웹에서 접근 가능한 파일의 최종 URL
     * @throws RuntimeException 파일 저장 중 오류가 발생한 경우
     */
    String save(byte[] fileData, String fileName);

    /**
     * 컨텍스트 정보를 포함하여 파일을 저장합니다.
     * 
     * @param fileData 저장할 파일의 바이트 데이터
     * @param fileName 저장할 파일의 이름 (확장자 포함)
     * @param context 파일 저장 컨텍스트 (사용자 ID, 작품 ID 등)
     * @return 웹에서 접근 가능한 파일의 최종 URL
     * @throws RuntimeException 파일 저장 중 오류가 발생한 경우
     */
    String saveWithContext(byte[] fileData, String fileName, FileStorageContext context);

    /**
     * 지정된 URL의 파일을 저장소에서 삭제합니다.
     * 
     * @param fileUrl 삭제할 파일의 웹 접근 URL
     * @throws RuntimeException 파일 삭제 중 오류가 발생한 경우
     */
    void deleteFile(String fileUrl);
}
