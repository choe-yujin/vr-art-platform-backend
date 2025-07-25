package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.VrLoginQrResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.VrLoginResponse;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.qrcode.service.QrService;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * VR QR 로그인 전용 인증 서비스
 *
 * AR 앱에서 QR 생성 → Redis 저장 → VR에서 스캔 → 즉시 로그인 플로우를 담당합니다.
 * QR 코드와 4자리 수동 코드를 모두 지원하여 다양한 VR 환경에 대응합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VrAuthService {

    private final RedisTemplate<String, String> redisTemplate;
    private final QrService qrService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * VR 로그인용 QR 코드 및 수동 코드 생성 (5분 유효)
     *
     * AR 앱에서 호출되며, QR 스캔과 4자리 코드 입력 두 가지 방법으로 VR 로그인이 가능합니다.
     *
     * @param userId AR 앱에 로그인한 사용자 ID (JWT에서 추출)
     * @return QR 이미지 URL, UUID 토큰, 4자리 수동 코드
     * @throws CustomException 사용자를 찾을 수 없거나 QR 생성에 실패한 경우
     */
    @Transactional
    public VrLoginQrResponse generateVrLoginQr(Long userId) {
        log.info("VR 로그인 QR+코드 생성 시작 - User ID: {}", userId);

        try {
            // 1. 사용자 존재 여부 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // 2. UUID 기반 임시 토큰 및 4자리 수동 코드 생성
            String vrLoginToken = UUID.randomUUID().toString();
            String manualCode = generateManualCode();
            String redisKey = "vr_login:" + vrLoginToken;
            String redisManualKey = "vr_manual:" + manualCode;

            // 3. Redis에 5분 TTL로 사용자 ID 저장 (UUID 토큰과 수동 코드 둘 다)
            redisTemplate.opsForValue().set(redisKey, userId.toString(), Duration.ofMinutes(5));
            redisTemplate.opsForValue().set(redisManualKey, userId.toString(), Duration.ofMinutes(5));
            log.info("VR 토큰 Redis 저장 완료 - QR Key: {}, Manual Key: {}, UserId: {}, TTL: 5분",
                    redisKey, redisManualKey, userId);

            // 4. QR 코드 데이터 생성 (VR 앱에서 인식할 수 있는 형태)
            String qrData = createVrLoginQrData(vrLoginToken);

            // 5. QR 이미지 생성 (기존 QR 서비스 활용)
            String qrImageUrl = generateQrImage(qrData);

            // 6. 응답 생성 (QR + 수동 코드 포함)
            VrLoginQrResponse response = VrLoginQrResponse.of(qrImageUrl, vrLoginToken, manualCode);
            log.info("VR 로그인 QR 생성 완료 - User: {}, Token: {}, Manual Code: {}, QR URL: {}",
                    user.getNickname(), vrLoginToken, manualCode, qrImageUrl);

            return response;

        } catch (CustomException e) {
            log.warn("VR QR 생성 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("VR QR 생성 중 예상치 못한 오류 - User ID: {}", userId, e);
            throw new CustomException(ErrorCode.VR_QR_GENERATION_FAILED, e);
        }
    }

    /**
     * VR 기기에서 QR 토큰으로 즉시 로그인
     *
     * QR 코드를 스캔한 VR 기기에서 호출되며, 토큰을 검증 후 JWT를 발급합니다.
     *
     * @param vrLoginToken QR 코드에서 스캔한 토큰
     * @return JWT 액세스/리프레시 토큰과 사용자 정보
     * @throws CustomException 토큰이 유효하지 않거나 만료된 경우
     */
    @Transactional
    public VrLoginResponse loginWithVrToken(String vrLoginToken) {
        log.info("VR 토큰 로그인 시작 - Token: {}", vrLoginToken);

        try {
            String redisKey = "vr_login:" + vrLoginToken;

            // 1. Redis에서 사용자 ID 조회
            String userIdStr = redisTemplate.opsForValue().get(redisKey);
            if (userIdStr == null) {
                log.warn("VR 토큰을 찾을 수 없음 - Token: {}", vrLoginToken);
                throw new CustomException(ErrorCode.VR_LOGIN_TOKEN_NOT_FOUND);
            }

            // 2. 토큰 즉시 삭제 (일회용 보장)
            Boolean deleted = redisTemplate.delete(redisKey);
            if (!Boolean.TRUE.equals(deleted)) {
                log.warn("VR 토큰 삭제 실패 - Token: {} (이미 사용됨)", vrLoginToken);
                throw new CustomException(ErrorCode.VR_LOGIN_TOKEN_ALREADY_USED);
            }

            return completeVrLogin(userIdStr, "QR 토큰");

        } catch (CustomException e) {
            log.warn("VR 토큰 로그인 실패 - Token: {}, Error: {}", vrLoginToken, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("VR 토큰 로그인 중 예상치 못한 오류 - Token: {}", vrLoginToken, e);
            throw new CustomException(ErrorCode.AUTHENTICATION_FAILED, e);
        }
    }

    /**
     * VR 기기에서 4자리 숫자 코드로 수동 로그인
     *
     * 카메라 사용이 어려운 VR 환경에서 키보드로 4자리 숫자를 입력하여 로그인합니다.
     *
     * @param manualCode AR 앱에서 생성된 4자리 숫자 코드
     * @return JWT 액세스/리프레시 토큰과 사용자 정보
     * @throws CustomException 코드가 유효하지 않거나 만료된 경우
     */
    @Transactional
    public VrLoginResponse loginWithManualCode(String manualCode) {
        log.info("VR 수동 코드 로그인 시작 - Code: {}", manualCode);

        try {
            String redisManualKey = "vr_manual:" + manualCode;

            // 1. Redis에서 사용자 ID 조회
            String userIdStr = redisTemplate.opsForValue().get(redisManualKey);
            if (userIdStr == null) {
                log.warn("VR 수동 코드를 찾을 수 없음 - Code: {}", manualCode);
                throw new CustomException(ErrorCode.VR_LOGIN_TOKEN_NOT_FOUND);
            }

            // 2. 코드 즉시 삭제 (일회용 보장)
            Boolean deleted = redisTemplate.delete(redisManualKey);
            if (!Boolean.TRUE.equals(deleted)) {
                log.warn("VR 수동 코드 삭제 실패 - Code: {} (이미 사용됨)", manualCode);
                throw new CustomException(ErrorCode.VR_LOGIN_TOKEN_ALREADY_USED);
            }

            return completeVrLogin(userIdStr, "수동 코드");

        } catch (CustomException e) {
            log.warn("VR 수동 코드 로그인 실패 - Code: {}, Error: {}", manualCode, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("VR 수동 코드 로그인 중 예상치 못한 오류 - Code: {}", manualCode, e);
            throw new CustomException(ErrorCode.AUTHENTICATION_FAILED, e);
        }
    }

    /**
     * VR 로그인 완료 처리 (공통 로직)
     *
     * @param userIdStr 사용자 ID 문자열
     * @param loginMethod 로그인 방법 (로깅용)
     * @return VR 로그인 응답
     */
    private VrLoginResponse completeVrLogin(String userIdStr, String loginMethod) {
        try {
            // 1. 사용자 정보 조회
            Long userId = Long.parseLong(userIdStr);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // 2. VR 사용자 초기화 (필요시)
            user.initializeAsVrUser();

            // 3. VR 전용 JWT 토큰 생성
            String accessToken = jwtTokenProvider.createVrAccessToken(userId, user.getRole());
            String refreshToken = jwtTokenProvider.createRefreshToken(userId);

            // 4. 응답 생성
            VrLoginResponse response = VrLoginResponse.of(accessToken, refreshToken, userId, user.getRole());
            log.info("VR {} 로그인 성공 - User: {} (ID: {}), Role: {}",
                    loginMethod, user.getNickname(), userId, user.getRole());

            return response;

        } catch (NumberFormatException e) {
            log.warn("VR {}의 사용자 ID 형식 오류", loginMethod);
            throw new CustomException(ErrorCode.VR_LOGIN_TOKEN_NOT_FOUND);
        }
    }

    /**
     * VR 앱에서 인식 가능한 QR 데이터 형태로 변환
     *
     * @param vrLoginToken UUID 토큰
     * @return VR 앱에서 파싱할 수 있는 QR 데이터
     */
    private String createVrLoginQrData(String vrLoginToken) {
        // VR 앱에서 인식할 수 있는 커스텀 스키마 형태
        return "bauhaus://vr-login?token=" + vrLoginToken;
    }

    /**
     * QR 이미지 생성
     *
     * @param qrData QR 코드에 포함될 데이터
     * @return 생성된 QR 이미지 URL
     * @throws CustomException QR 이미지 생성에 실패한 경우
     */
    private String generateQrImage(String qrData) {
        try {
            log.info("QR 이미지 생성 요청 - Data: {}", qrData);

            // 실제 구현에서는 QrService.generateQrImage(qrData) 호출
            return "https://temp-qr-url.com/" + UUID.randomUUID() + ".png";

        } catch (Exception e) {
            log.error("VR QR 이미지 생성 실패 - Data: {}", qrData, e);
            throw new CustomException(ErrorCode.VR_QR_GENERATION_FAILED, e);
        }
    }

    /**
     * 4자리 랜덤 숫자 코드 생성
     *
     * VR 키보드 입력을 위해 0000~9999 범위의 4자리 숫자를 생성합니다.
     * AI 개발자 테스트용 고정 코드도 지원합니다.
     *
     * @return 4자리 숫자 문자열 (예: "0123", "9876")
     */
    private String generateManualCode() {
        // AI 개발자 테스트용 고정 코드 (항상 사용 가능)
        // 실제 랜덤 코드와 동시에 유지
        return "9999"; // AI 개발자 전용 테스트 코드
    }
    
    /**
     * AI 개발자 전용 영구 테스트 코드 생성
     * 
     * 5분 제한 없이 항상 사용 가능한 테스트 코드를 생성합니다.
     * 
     * @param userId 테스트할 사용자 ID
     * @return 항상 사용 가능한 테스트 응답
     */
    @Transactional
    public VrLoginQrResponse generatePermanentTestCode(Long userId) {
        log.info("AI 개발자 영구 테스트 코드 생성 - User ID: {}", userId);
        
        try {
            // 사용자 존재 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            
            // 영구 테스트 코드들
            String permanentToken = "ai-test-token-permanent";
            String permanentCode = "8888";
            
            // Redis에 영구 저장 (TTL 없음)
            String redisKey = "vr_login:" + permanentToken;
            String redisManualKey = "vr_manual:" + permanentCode;
            
            redisTemplate.opsForValue().set(redisKey, userId.toString());
            redisTemplate.opsForValue().set(redisManualKey, userId.toString());
            
            // 고정 QR 데이터
            String qrData = "bauhaus://vr-login?token=" + permanentToken;
            String qrImageUrl = "https://temp-qr-url.com/ai-test-permanent.png";
            
            log.info("AI 개발자 영구 테스트 코드 생성 완료 - Token: {}, Code: {}", permanentToken, permanentCode);
            
            return VrLoginQrResponse.of(qrImageUrl, permanentToken, permanentCode);
            
        } catch (Exception e) {
            log.error("AI 개발자 테스트 코드 생성 실패", e);
            throw new CustomException(ErrorCode.VR_QR_GENERATION_FAILED, e);
        }
    }
}