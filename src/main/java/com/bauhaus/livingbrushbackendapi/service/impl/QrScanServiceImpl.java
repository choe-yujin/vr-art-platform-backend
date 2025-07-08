package com.bauhaus.livingbrushbackendapi.service.impl;

import com.bauhaus.livingbrushbackendapi.dto.response.ArtworkResponse;
import com.bauhaus.livingbrushbackendapi.entity.QrCode;
import com.bauhaus.livingbrushbackendapi.exception.QrCodeNotFoundException;
import com.bauhaus.livingbrushbackendapi.repository.QrCodeRepository;
import com.bauhaus.livingbrushbackendapi.service.interfaces.QrScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrScanServiceImpl implements QrScanService {

    private final QrCodeRepository qrCodeRepository;

    @Override
    @Transactional(readOnly = true)
    public ArtworkResponse validateAndGetArtwork(UUID qrToken) {
        QrCode qrCode = qrCodeRepository.findByQrTokenAndIsActiveTrue(qrToken)
                .orElseThrow(() -> new QrCodeNotFoundException(qrToken.toString()));

        // QrCode 엔티티에서 Artwork 엔티티를 가져와 ArtworkResponse DTO로 변환하여 반환
        return ArtworkResponse.from(qrCode.getArtwork());
    }
}