package com.bauhaus.livingbrushbackendapi.qrcode.service.impl;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import com.bauhaus.livingbrushbackendapi.artwork.repository.ArtworkRepository;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageService;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageContext;
import com.bauhaus.livingbrushbackendapi.config.AppProperties;
// [수정] CustomException과 ErrorCode를 임포트합니다.
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.qrcode.dto.QrGenerateResponse;
import com.bauhaus.livingbrushbackendapi.qrcode.entity.QrCode;
import com.bauhaus.livingbrushbackendapi.qrcode.repository.QrCodeRepository;
import com.bauhaus.livingbrushbackendapi.qrcode.service.QrService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * 로컬 QR 코드 생성 서비스 (리팩토링 v2.0)
 *
 * 예외 처리 방식을 CustomException으로 통일하여 애플리케이션의 일관성을 확보합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalQrService implements QrService {

    private final QrCodeRepository qrCodeRepository;
    private final ArtworkRepository artworkRepository;
    private final AppProperties appProperties;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public QrGenerateResponse generateQr(Long artworkId) {
        log.info("QR 생성 요청 - 작품 ID: {}", artworkId);

        // 1. 작품 조회 및 유효성 검증
        Artwork artwork = findAndValidateArtwork(artworkId);

        // 2. 기존 QR 코드 비활성화
        deactivateExistingQrCodes(artworkId);

        // 3. 새로운 QR 코드 정보 생성
        UUID qrToken = generateUniqueToken();
        String webArUrl = createWebArUrl(artworkId);

        // 4. QR 코드 이미지 생성 및 저장
        String qrImageUrl = createAndStoreQrImage(qrToken, webArUrl, artwork);

        // 5. 새로운 QR 코드 엔티티 저장
        saveNewQrCode(artwork, qrToken, qrImageUrl);

        log.info("QR 생성 완료 - 토큰: {}, 이미지 URL: {}", qrToken, qrImageUrl);
        return new QrGenerateResponse(qrImageUrl);
    }

    /**
     * 작품을 조회하고 QR 코드 생성 가능 여부를 검증합니다.
     */
    private Artwork findAndValidateArtwork(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                // [수정] DomainException -> CustomException
                .orElseThrow(() -> new CustomException(ErrorCode.ARTWORK_NOT_FOUND));

        if (artwork.getVisibility() != VisibilityType.PUBLIC) {
            // [수정] DomainException -> CustomException
            throw new CustomException(ErrorCode.ARTWORK_NOT_PUBLIC);
        }
        return artwork;
    }

    /**
     * QR 코드 이미지를 생성하고 파일 시스템에 저장합니다.
     */
    private String createAndStoreQrImage(UUID qrToken, String webArUrl, Artwork artwork) {
        try {
            AppProperties.Qr qrConfig = appProperties.getQr();

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(webArUrl, BarcodeFormat.QR_CODE,
                    qrConfig.getSize(), qrConfig.getSize());

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, qrConfig.getFormat(), pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            String fileName = qrToken.toString() + "." + qrConfig.getFormat().toLowerCase();
            
            // 컨텍스트 정보를 포함하여 저장
            FileStorageContext context = FileStorageContext.forQrCode(
                    artwork.getUser().getUserId(), 
                    artwork.getArtworkId()
            );
            
            return fileStorageService.saveWithContext(pngData, fileName, context);

        } catch (WriterException | IOException e) {
            log.error("QR 코드 이미지 데이터 생성 또는 저장 중 오류 발생. Token: {}", qrToken, e);
            // [수정] InfrastructureException -> CustomException
            throw new CustomException(ErrorCode.QR_GENERATION_FAILED, e);
        }
    }

    /**
     * 해당 작품에 연결된 모든 기존 QR 코드를 비활성화합니다.
     */
    private void deactivateExistingQrCodes(Long artworkId) {
        int deactivatedCount = qrCodeRepository.deactivateAllByArtworkId(artworkId);
        if (deactivatedCount > 0) {
            log.info("기존 활성 QR 코드 {} 개 비활성화 완료. 작품 ID: {}", deactivatedCount, artworkId);
        }
    }

    /**
     * 데이터베이스에 존재하지 않는 고유한 UUID 토큰을 생성합니다.
     */
    private UUID generateUniqueToken() {
        UUID token;
        do {
            token = UUID.randomUUID();
        } while (qrCodeRepository.existsByQrToken(token));
        return token;
    }

    /**
     * Web AR 페이지로 연결되는 URL을 생성합니다.
     */
    private String createWebArUrl(Long artworkId) {
        return String.format("%s/%d", appProperties.getWebAr().getBaseUrl(), artworkId);
    }

    /**
     * 새로운 QR 코드 정보를 데이터베이스에 저장합니다.
     */
    private void saveNewQrCode(Artwork artwork, UUID qrToken, String qrImageUrl) {
        QrCode qrCode = QrCode.builder()
                .artwork(artwork)
                .qrToken(qrToken)
                .qrImageUrl(qrImageUrl)
                .isActive(true)
                .build();
        qrCodeRepository.save(qrCode);
    }
}