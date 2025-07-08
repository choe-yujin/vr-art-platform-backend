package com.bauhaus.livingbrushbackendapi.service.interfaces;

import com.bauhaus.livingbrushbackendapi.dto.response.QrGenerateResponse;

/**
 * QR 코드 생성 서비스 인터페이스
 * 
 * 작품에 대한 QR 코드 생성 및 관리 기능을 정의합니다.
 * 환경별로 다른 구현체를 사용할 수 있도록 추상화되었습니다.
 * 
 * 구현체:
 * - LocalQrService: 로컬 파일 시스템 저장 (개발용)
 * - S3QrService: AWS S3 저장 (운영용)
 */
public interface QrService {

    /**
     * 작품에 대한 QR 코드를 생성합니다.
     * 
     * 비즈니스 규칙:
     * - 공개(public) 작품만 QR 생성 가능 (DB 트리거에서 검증)
     * - 고유한 QR 토큰 자동 생성 (UUID)
     * - QR 이미지 생성 및 저장 (ZXing 라이브러리)
     * - qr_codes 테이블에 레코드 저장
     * 
     * @param artworkId QR 코드를 생성할 작품의 ID
     * @return QR 이미지 URL을 포함한 응답 객체
     * @throws ArtworkNotFoundException 작품이 존재하지 않는 경우
     * @throws QrGenerationException QR 생성 중 오류가 발생한 경우
     */
    QrGenerateResponse generateQr(Long artworkId);
}
