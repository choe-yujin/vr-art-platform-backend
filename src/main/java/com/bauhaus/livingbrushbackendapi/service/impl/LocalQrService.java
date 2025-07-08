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
import com.bauhaus.livingbrushbackendapi.service.interfaces.QrService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@Profile("local")
@RequiredArgsConstructor
public class LocalQrService implements QrService {

    private final QrCodeRepository qrCodeRepository;
    private final ArtworkRepository artworkRepository;
    private final AppProperties appProperties; // @ConfigurationProperties 주입

    @Override
    @Transactional
    public QrGenerateResponse generateQr(Long artworkId) {
        log.info("QR 생성 요청 - 작품 ID: {}", artworkId);

        Artwork artwork = findAndValidateArtwork(artworkId);

        deactivateExistingQrCodes(artworkId);

        UUID qrToken = generateUniqueToken();
        String webArUrl = String.format("%s/%d", appProperties.getWebAr().getBaseUrl(), artworkId);
        String qrImageUrl = createAndSaveQrImage(qrToken, webArUrl);

        saveNewQrCode(artwork, qrToken, qrImageUrl);

        log.info("QR 생성 완료 - 토큰: {}, 이미지 URL: {}", qrToken, qrImageUrl);
        return QrGenerateResponse.of(qrImageUrl);
    }

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
        // UUID는 충돌 확률이 극히 낮지만, DB에 존재하지 않는 것을 보장하는 로직
        UUID token;
        do {
            token = UUID.randomUUID();
        } while (qrCodeRepository.existsByQrToken(token)); // 이 메소드는 QrCodeRepository에 추가 필요
        return token;
    }

    private String createAndSaveQrImage(UUID qrToken, String webArUrl) {
        try {
            AppProperties.Qr qrConfig = appProperties.getQr();
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(webArUrl, BarcodeFormat.QR_CODE, qrConfig.getSize(), qrConfig.getSize());

            Path directoryPath = Path.of(qrConfig.getLocalPath());
            Files.createDirectories(directoryPath); // 디렉토리가 없으면 생성

            String fileName = qrToken.toString() + "." + qrConfig.getFormat().toLowerCase();
            Path filePath = directoryPath.resolve(fileName);

            MatrixToImageWriter.writeToPath(bitMatrix, qrConfig.getFormat(), filePath);

            // 정적 파일 제공 경로를 기반으로 웹 접근 URL 반환
            return "/qr-images/" + fileName;
        } catch (WriterException | IOException e) {
            log.error("QR 코드 이미지 생성 또는 저장 중 오류 발생. Token: {}", qrToken, e);
            throw QrGenerationException.forInfrastructure("QR 코드 이미지 생성에 실패했습니다.", e);
        }
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