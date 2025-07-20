package com.bauhaus.livingbrushbackendapi.qrcode.service;

import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkResponse;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;

import java.util.UUID;

/**
 * QR 코드 스캔 서비스 인터페이스 (리팩토링 v2.0)
 *
 * 예외 처리 방식을 CustomException으로 통일하여 애플리케이션의 일관성을 확보합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
public interface QrScanService {

    /**
     * QR 토큰을 검증하고 연결된 작품 정보를 반환합니다.
     *
     * @param qrToken 검증할 QR 토큰 (UUID)
     * @return 연결된 작품 정보 DTO
     * @throws CustomException QR 코드가 존재하지 않거나(예: ErrorCode.QR_CODE_NOT_FOUND),
     *                         만료 또는 비활성화된 경우(예: ErrorCode.QR_CODE_EXPIRED).
     */
    ArtworkResponse validateAndGetArtwork(UUID qrToken);
}