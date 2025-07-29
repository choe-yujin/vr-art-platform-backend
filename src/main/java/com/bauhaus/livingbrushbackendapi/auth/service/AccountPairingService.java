package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.PairingRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.PairingResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.PairingStatusResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.entity.AccountPairing;
import com.bauhaus.livingbrushbackendapi.auth.entity.AccountLinkingHistory;
import com.bauhaus.livingbrushbackendapi.auth.repository.AccountPairingRepository;
import com.bauhaus.livingbrushbackendapi.auth.repository.AccountLinkingHistoryRepository;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageService;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageContext;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 계정 페어링 서비스
 * 
 * VR-AR 계정 연동을 위한 QR 코드 기반 페어링을 처리합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@Service
@Transactional
public class AccountPairingService {

    private final AccountPairingRepository pairingRepository;
    private final AccountLinkingHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileStorageService fileStorageService;
    private final RestTemplate metaRestTemplate;
    
    @Value("${spring.security.oauth2.client.provider.meta.user-info-uri}")
    private String metaUserInfoUri;

    private static final int PAIRING_EXPIRATION_MINUTES = 5; // 5분 만료
    private static final int QR_CODE_SIZE = 256; // QR 코드 크기

    public AccountPairingService(
            AccountPairingRepository pairingRepository,
            AccountLinkingHistoryRepository historyRepository,
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            FileStorageService fileStorageService,
            @Qualifier("metaRestTemplate") RestTemplate metaRestTemplate) {
        this.pairingRepository = pairingRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.fileStorageService = fileStorageService;
        this.metaRestTemplate = metaRestTemplate;
    }

    /**
     * AR 앱에서 페어링 코드를 생성합니다.
     * 
     * @param arUserId AR 앱 사용자 ID
     * @return 페어링 코드와 QR 이미지 URL
     */
    public PairingResponse generatePairingCode(Long arUserId) {
        log.info("🔗 페어링 코드 생성 요청 - AR 사용자 ID: {}", arUserId);

        // 1. AR 사용자 조회
        User arUser = userRepository.findById(arUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 기존 활성 페어링 요청 무효화
        invalidateExistingPairings(arUser);

        // 3. 새로운 페어링 요청 생성
        AccountPairing pairing = AccountPairing.createPairingRequest(arUser, PAIRING_EXPIRATION_MINUTES);
        
        // 4. QR 코드 이미지 생성
        String qrImageUrl = generateQrCodeImage(pairing.getPairingCodeString(), arUser.getUserId());
        pairing.setQrImageUrl(qrImageUrl);

        // 5. 데이터베이스에 저장
        pairingRepository.save(pairing);

        log.info("✅ 페어링 코드 생성 완료 - 코드: {}, 만료: {}", 
                pairing.getShortPairingCode(), pairing.getExpiresAt());

        return PairingResponse.from(pairing);
    }

    /**
     * VR 앱에서 페어링을 확인하고 계정 연동을 수행합니다.
     * 
     * @param request VR 페어링 요청 정보
     * @return 연동 완료된 사용자의 인증 응답
     */
    public AuthResponse confirmPairing(PairingRequest request) {
        log.info("🔗 페어링 확인 요청 - 코드: {}, Meta 사용자: {}", 
                request.getPairingCode(), request.getMetaUserId());

        // 1. 페어링 코드 검증
        UUID pairingCode = parseAndValidatePairingCode(request.getPairingCode());
        AccountPairing pairing = pairingRepository.findValidPairingByCode(pairingCode, LocalDateTime.now())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_PAIRING_CODE));

        // 2. Meta Access Token 검증
        validateMetaToken(request.getMetaAccessToken(), request.getMetaUserId());

        // 3. Meta 계정 중복 연동 확인
        validateMetaAccountNotLinked(request.getMetaUserId());

        // 4. 페어링 완료 처리
        pairing.completePairing(request.getMetaUserId());
        pairingRepository.save(pairing);

        // 5. AR 사용자 계정에 Meta 계정 연동
        User arUser = pairing.getArUser();
        UserRole previousRole = arUser.getRole();
        
        arUser.linkOAuthAccount(Provider.META, request.getMetaUserId());
        arUser.promoteToArtist(); // GUEST/USER → ARTIST 승격
        userRepository.save(arUser);

        // 6. 연동 이력 기록
        saveAccountLinkingHistory(arUser, request.getMetaUserId(), previousRole);

        // 7. 새로운 JWT 토큰 발급 (ARTIST 권한)
        String accessToken = jwtTokenProvider.createAccessToken(arUser.getUserId(), arUser.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(arUser.getUserId());

        log.info("✅ 페어링 완료 - AR 사용자: {}, Meta 사용자: {}, 권한: {} → {}", 
                arUser.getUserId(), request.getMetaUserId(), previousRole, arUser.getRole());

        return new AuthResponse(accessToken, refreshToken, arUser.getUserId(), arUser.getRole(), false);
    }

    /**
     * 페어링 상태를 조회합니다.
     * 
     * @param pairingCode 페어링 코드
     * @return 페어링 상태 정보
     */
    @Transactional(readOnly = true)
    public PairingStatusResponse getPairingStatus(String pairingCode) {
        UUID code = parseAndValidatePairingCode(pairingCode);
        
        return pairingRepository.findByPairingCode(code)
                .map(pairing -> {
                    if (pairing.isCompleted()) {
                        return PairingStatusResponse.completed(
                                pairing.getLinkedMetaUserId(), 
                                pairing.getCompletedAt()
                        );
                    } else if (pairing.isExpired()) {
                        return PairingStatusResponse.expired();
                    } else {
                        return PairingStatusResponse.pending();
                    }
                })
                .orElse(PairingStatusResponse.invalid());
    }

    /**
     * 만료된 페어링 요청들을 정리합니다.
     * 
     * @return 삭제된 레코드 수
     */
    public int cleanupExpiredPairings() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusHours(1); // 1시간 전
        int deletedCount = pairingRepository.deleteExpiredPairings(expiredBefore);
        
        if (deletedCount > 0) {
            log.info("🧹 만료된 페어링 요청 {} 개 정리 완료", deletedCount);
        }
        
        return deletedCount;
    }

    // ========== Private Helper Methods ==========

    /**
     * 사용자의 기존 활성 페어링 요청들을 무효화합니다.
     */
    private void invalidateExistingPairings(User user) {
        int invalidatedCount = pairingRepository.invalidateUserPairings(user);
        if (invalidatedCount > 0) {
            log.info("기존 활성 페어링 요청 {} 개 무효화", invalidatedCount);
        }
    }

    /**
     * QR 코드 이미지를 생성하고 S3에 저장합니다.
     */
    private String generateQrCodeImage(String pairingCode, Long userId) {
        try {
            // 1. QR 코드 데이터 생성 (페어링 URL 형태)
            String qrData = createPairingUrl(pairingCode);
            
            // 2. QR 코드 이미지 생성
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            // 3. PNG 바이트 배열로 변환
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            // 4. S3에 저장
            String fileName = "pairing-" + pairingCode.substring(0, 8) + ".png";
            FileStorageContext context = FileStorageContext.forPairingQr(userId);
            
            return fileStorageService.saveWithContext(pngData, fileName, context);

        } catch (WriterException | IOException e) {
            log.error("QR 코드 이미지 생성 실패 - 페어링 코드: {}", pairingCode, e);
            throw new CustomException(ErrorCode.QR_GENERATION_FAILED, e);
        }
    }

    /**
     * 페어링용 URL을 생성합니다.
     */
    private String createPairingUrl(String pairingCode) {
        // VR 앱에서 인식할 수 있는 커스텀 URL 스키마
        return String.format("bauhaus://pairing?code=%s", pairingCode);
    }

    /**
     * 페어링 코드 문자열을 UUID로 파싱하고 검증합니다.
     */
    private UUID parseAndValidatePairingCode(String pairingCodeStr) {
        try {
            // 짧은 코드인 경우 전체 UUID 조회 필요 (이 예제에서는 단순화)
            if (pairingCodeStr.length() == 8) {
                // 실제로는 DB에서 Short Code로 검색해야 함
                throw new CustomException(ErrorCode.INVALID_PAIRING_CODE, "전체 페어링 코드를 입력해주세요.");
            }
            
            return UUID.fromString(pairingCodeStr);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_PAIRING_CODE, "올바르지 않은 페어링 코드 형식입니다.");
        }
    }

    /**
     * Meta Access Token을 검증합니다.
     */
    private void validateMetaToken(String metaAccessToken, String expectedMetaUserId) {
        try {
            String url = metaUserInfoUri + "?access_token=" + metaAccessToken;
            var response = metaRestTemplate.getForEntity(url, MetaUserInfo.class);
            MetaUserInfo userInfo = response.getBody();

            if (userInfo == null || !expectedMetaUserId.equals(userInfo.id())) {
                throw new CustomException(ErrorCode.INVALID_TOKEN, "Meta 토큰이 사용자 ID와 일치하지 않습니다.");
            }
        } catch (Exception e) {
            log.error("Meta 토큰 검증 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Meta Access Token입니다.");
        }
    }

    /**
     * Meta 계정이 이미 다른 사용자와 연동되었는지 확인합니다.
     */
    private void validateMetaAccountNotLinked(String metaUserId) {
        if (pairingRepository.existsByLinkedMetaUserIdAndIsUsedTrue(metaUserId)) {
            throw new CustomException(ErrorCode.META_ACCOUNT_ALREADY_TAKEN, 
                    "이 Meta 계정은 이미 다른 사용자와 연동되어 있습니다.");
        }
    }

    /**
     * 계정 연동 이력을 저장합니다.
     */
    private void saveAccountLinkingHistory(User user, String metaUserId, UserRole previousRole) {
        // 연동 이력 기록
        AccountLinkingHistory linkingHistory = AccountLinkingHistory.createLinkingRecord(
                user, Provider.META, metaUserId, previousRole, user.getRole(), null);
        historyRepository.save(linkingHistory);

        // 권한 승격이 발생한 경우 추가 이력 기록
        if (previousRole != user.getRole()) {
            AccountLinkingHistory promotionHistory = AccountLinkingHistory.createPromotionRecord(
                    user, Provider.META, metaUserId, previousRole, user.getRole());
            historyRepository.save(promotionHistory);
        }
    }

    /**
     * Meta API 응답용 내부 DTO
     */
    private record MetaUserInfo(String id, String name, String email) {}
}
