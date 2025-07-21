package com.bauhaus.livingbrushbackendapi.qrcode.service.impl;

import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkResponse;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.qrcode.entity.QrCode;
import com.bauhaus.livingbrushbackendapi.qrcode.repository.QrCodeRepository;
import com.bauhaus.livingbrushbackendapi.qrcode.service.QrScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * QR 코드 스캔 서비스 구현체 (리팩토링 v2.1)
 *
 * 예외 처리 방식을 CustomException으로 통일하고,
 * 오류 상황(존재하지 않음, 비활성화)을 명확히 구분하여 사용자에게 더 나은 피드백을 제공합니다.
 *
 * @author Bauhaus Team
 * @version 2.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QrScanServiceImpl implements QrScanService {

    private final QrCodeRepository qrCodeRepository;

    @Override
    @Transactional(readOnly = true)
    public ArtworkResponse validateAndGetArtwork(UUID qrToken) {
        log.info("QR 코드 검증 요청. Token: {}", qrToken);

        // 1. 토큰으로 QR 코드를 먼저 조회합니다.
        QrCode qrCode = qrCodeRepository.findByQrToken(qrToken)
                // 2. 조회 결과가 없으면 '존재하지 않음' 예외를 발생시킵니다.
                .orElseThrow(() -> new CustomException(ErrorCode.QR_CODE_NOT_FOUND));

        // 3. QR 코드가 비활성화 상태이면 '비활성화' 예외를 발생시킵니다.
        if (!qrCode.isActive()) {
            log.warn("비활성화된 QR 코드 스캔 시도. Token: {}", qrToken);
            throw new CustomException(ErrorCode.QR_CODE_INACTIVE);
        }

        // 🔥 4. 작품이 공개 상태인지 추가 검증
        if (!qrCode.getArtwork().isPublic()) {
            log.warn("비공개 작품의 QR 코드 스캔 시도. Token: {}, 작품 ID: {}", 
                    qrToken, qrCode.getArtwork().getArtworkId());
            throw new CustomException(ErrorCode.QR_CODE_INACTIVE);
        }

        // 5. 모든 검증을 통과하면 작품 정보를 반환합니다.
        log.info("QR 코드 검증 성공. 작품 ID: {}", qrCode.getArtwork().getArtworkId());
        return ArtworkResponse.from(qrCode.getArtwork());
    }
}