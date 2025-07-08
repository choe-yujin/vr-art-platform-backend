package com.bauhaus.livingbrushbackendapi.service.impl;

import com.bauhaus.livingbrushbackendapi.config.AppProperties;
import com.bauhaus.livingbrushbackendapi.dto.response.QrGenerateResponse;
import com.bauhaus.livingbrushbackendapi.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.entity.QrCode;
import com.bauhaus.livingbrushbackendapi.entity.enumeration.VisibilityType;
import com.bauhaus.livingbrushbackendapi.exception.ArtworkNotFoundException;
import com.bauhaus.livingbrushbackendapi.exception.QrGenerationException;
import com.bauhaus.livingbrushbackendapi.repository.ArtworkRepository;
import com.bauhaus.livingbrushbackendapi.repository.QrCodeRepository;
import com.bauhaus.livingbrushbackendapi.service.interfaces.FileStorageService; // 새로 추가
import com.bauhaus.livingbrushbackendapi.service.interfaces.QrService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream; // 새로 추가
import java.io.IOException; // 새로 추가
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalQrService implements QrService {

    private final QrCodeRepository qrCodeRepository;
    private final ArtworkRepository artworkRepository;
    private final AppProperties appProperties;
    private final FileStorageService fileStorageService; // ✨ FileStorageService 의존성 주입

    @Override
    @Transactional
    public QrGenerateResponse generateQr(Long artworkId) {
        log.info("QR 생성 요청 - 작품 ID: {}", artworkId);

        Artwork artwork = findAndValidateArtwork(artworkId);
        deactivateExistingQrCodes(artworkId);

        UUID qrToken = generateUniqueToken();
        String webArUrl = String.format("%s/%d", appProperties.getWebAr().getBaseUrl(), artworkId);

        // ✨ 파일 저장 로직을 FileStorageService에 위임
        String qrImageUrl = createAndStoreQrImage(qrToken, webArUrl);

        saveNewQrCode(artwork, qrToken, qrImageUrl);

        log.info("QR 생성 완료 - 토큰: {}, 이미지 URL: {}", qrToken, qrImageUrl);
        return QrGenerateResponse.of(qrImageUrl);
    }

    // ✨ 메소드 이름 변경 및 책임 분리
    private String createAndStoreQrImage(UUID qrToken, String webArUrl) {
        try {
            AppProperties.Qr qrConfig = appProperties.getQr();

            // 1. QR 코드 데이터(BitMatrix) 생성
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(webArUrl, BarcodeFormat.QR_CODE, qrConfig.getSize(), qrConfig.getSize());

            // 2. BitMatrix를 byte[]로 변환
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, qrConfig.getFormat(), pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            // 3. 파일 이름 생성 및 FileStorageService를 통해 저장
            String fileName = qrToken.toString() + "." + qrConfig.getFormat().toLowerCase();
            return fileStorageService.save(pngData, fileName);

        } catch (WriterException | IOException e) {
            log.error("QR 코드 이미지 데이터 생성 또는 저장 중 오류 발생. Token: {}", qrToken, e);
            throw QrGenerationException.forInfrastructure("QR 코드 이미지 생성에 실패했습니다.", e);
        }
    }

    // --- 아래 private 메소드들은 변경 없음 ---

    private Artwork findAndValidateArtwork(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkNotFoundException(artworkId));

        if (artwork.getVisibility() != VisibilityType.PUBLIC) {
            throw QrGenerationException.forBusinessRule("공개(PUBLIC) 상태인 작품만 QR 코드 생성이 가능합니다.");
        }
        return artwork;
    }

    private void deactivateExistingQrCodes(Long artworkId) {
        int deactivatedCount = qrCodeRepository.deactivateAllByArtworkId(artworkId);
        if (deactivatedCount > 0) {
            log.info("기존 활성 QR 코드 {} 개 비활성화", deactivatedCount);
        }
    }

    private UUID generateUniqueToken() {
        UUID token;
        do {
            token = UUID.randomUUID();
        } while (qrCodeRepository.existsByQrToken(token));
        return token;
    }

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