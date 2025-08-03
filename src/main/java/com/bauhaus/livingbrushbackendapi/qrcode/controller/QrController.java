package com.bauhaus.livingbrushbackendapi.qrcode.controller;

import com.bauhaus.livingbrushbackendapi.qrcode.dto.QrGenerateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkResponse;
import com.bauhaus.livingbrushbackendapi.qrcode.dto.QrGenerateResponse;
import com.bauhaus.livingbrushbackendapi.qrcode.service.QrScanService;
import com.bauhaus.livingbrushbackendapi.qrcode.service.QrService;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * QR 코드 관리 컨트롤러
 *
 * QR 코드 생성(generate)과 스캔(scan)에 대한 API 엔드포인트를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
@Tag(name = "QR Code", description = "QR 코드 생성 및 스캔 API")
public class QrController {

    private final QrService qrService;
    private final QrScanService qrScanService; // 스캔/검증 책임을 가진 서비스
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * QR 코드 생성 API
     *
     * 공개 작품에 대해 WebAR 뷰어로 연결되는 QR 코드를 생성합니다.
     *
     * @param request QR 생성 요청 (작품 ID 포함)
     * @return 생성된 QR 이미지의 URL을 포함한 응답
     */
    @Operation(
            summary = "QR 코드 생성",
            description = "공개 작품에 대해 WebAR 뷰어로 연결되는 QR 코드를 생성합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PostMapping("/generate")
    public ResponseEntity<QrGenerateResponse> generateQr(
            @Valid @RequestBody QrGenerateRequest request,
            Authentication authentication
    ) {
        // JWT 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("QR 생성 API 호출 - 작품 ID: {}, 사용자 ID: {}", request.getArtworkId(), userId);
        QrGenerateResponse response = qrService.generateQr(request.getArtworkId());

        // [수정] record의 접근자 메소드 이름은 get... 이 아닌 필드명과 동일합니다.
        log.info("QR 생성 성공 - 이미지 URL: {}", response.qrImageUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * QR 코드 스캔 및 작품 정보 조회 API
     *
     * WebAR 뷰어 또는 다른 클라이언트가 QR 코드를 스캔했을 때 호출됩니다.
     * 유효한 QR 토큰을 받아 해당하는 작품 정보를 반환합니다.
     *
     * @param qrToken 스캔된 QR 코드의 고유 토큰 (UUID)
     * @return 작품 정보를 포함한 응답 (ArtworkResponse)
     */
    @Operation(
            summary = "QR 코드 스캔",
            description = "QR 코드를 스캔하여 작품 정보를 조회합니다. 인증 불필요 (공개 접근)"
    )
    @GetMapping("/scan/{qrToken}")
    public ResponseEntity<ArtworkResponse> scanQrCode(@PathVariable UUID qrToken) {
        log.info("QR 스캔 API 호출 - 토큰: {}", qrToken);
        // 주입된 qrScanService를 사용하여 로직 수행
        ArtworkResponse response = qrScanService.validateAndGetArtwork(qrToken);
        log.info("QR 스캔 성공 - 작품 ID: {}", response.getArtworkId());
        return ResponseEntity.ok(response);
    }
}