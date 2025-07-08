package com.bauhaus.livingbrushbackendapi.service.interfaces;

import com.bauhaus.livingbrushbackendapi.dto.response.ArtworkResponse;
import java.util.UUID;

public interface QrScanService {
    /**
     * QR 토큰을 검증하고 연결된 작품 정보를 반환합니다.
     * @param qrToken 검증할 QR 토큰
     * @return 작품 정보 DTO
     */
    ArtworkResponse validateAndGetArtwork(UUID qrToken);
}