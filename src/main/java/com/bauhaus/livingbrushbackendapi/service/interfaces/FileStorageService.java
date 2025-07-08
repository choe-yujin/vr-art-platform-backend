package com.bauhaus.livingbrushbackendapi.service.interfaces;

/**
 * 파일 저장 로직을 추상화하는 서비스 인터페이스입니다.
 * 구현체는 로컬 파일 시스템, 클라우드 스토리지(S3) 등 다양할 수 있습니다.
 */
public interface FileStorageService {

    /**
     * 파일 데이터를 저장소에 저장합니다.
     *
     * @param fileData 저장할 파일의 바이트 데이터
     * @param fileName 저장할 파일의 이름 (확장자 포함)
     * @return 웹에서 접근 가능한 파일의 최종 URL
     */
    String save(byte[] fileData, String fileName);
}