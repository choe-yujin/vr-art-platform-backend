package com.bauhaus.livingbrushbackendapi.security.jwt;

import com.bauhaus.livingbrushbackendapi.entity.enumeration.UserRole;
import com.bauhaus.livingbrushbackendapi.exception.TokenValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰 제공자
 *
 * JWT 토큰의 생성, 검증, 파싱을 담당
 *
 * 주요 기능:
 * - Access Token 생성 (30분 유효)
 * - Refresh Token 생성 (7일 유효)
 * - 토큰 검증 및 사용자 정보 추출
 * - 토큰 갱신 처리
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    // JWT 클레임 키 상수
    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLE_CLAIM = "role";
    private static final String PLATFORM_CLAIM = "platform";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    // 토큰 타입 상수
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity:1800000}") long accessTokenValidity, // 30분 기본값
            @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidity // 7일 기본값
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMilliseconds = accessTokenValidity;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidity;

        log.info("JWT 토큰 제공자 초기화 완료 - Access Token 유효시간: {}ms, Refresh Token 유효시간: {}ms",
                accessTokenValidity, refreshTokenValidity);
    }

    /**
     * Access Token 생성 (새로운 메서드명으로 통일)
     *
     * @param userId 사용자 ID
     * @param userRole 사용자 권한
     * @return Access Token
     */
    public String createAccessToken(Long userId, UserRole userRole) {
        return generateAccessToken(userId, userRole, null);
    }

    /**
     * Refresh Token 생성 (새로운 메서드명으로 통일)
     *
     * @param userId 사용자 ID
     * @return Refresh Token
     */
    public String createRefreshToken(Long userId) {
        return generateRefreshToken(userId);
    }

    /**
     * Access Token 생성 (플랫폼 정보 포함)
     *
     * @param userId 사용자 ID
     * @param userRole 사용자 권한
     * @param platform 플랫폼 정보 (vr, ar)
     * @return Access Token
     */
    public String generateAccessToken(Long userId, UserRole userRole, String platform) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, userId);
        claims.put(ROLE_CLAIM, userRole.name());
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);

        if (platform != null) {
            claims.put(PLATFORM_CLAIM, platform);
        }

        String token = Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        log.debug("Access Token 생성 완료 - 사용자 ID: {}, 권한: {}, 플랫폼: {}, 만료시간: {}",
                userId, userRole, platform, expiryDate);

        return token;
    }

    /**
     * Refresh Token 생성
     *
     * @param userId 사용자 ID
     * @return Refresh Token
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, userId);
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);

        String token = Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        log.debug("Refresh Token 생성 완료 - 사용자 ID: {}, 만료시간: {}", userId, expiryDate);

        return token;
    }

    /**
     * 토큰에서 사용자 ID 추출
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object userIdClaim = claims.get(USER_ID_CLAIM);

        if (userIdClaim instanceof Integer) {
            return ((Integer) userIdClaim).longValue();
        } else if (userIdClaim instanceof Long) {
            return (Long) userIdClaim;
        } else {
            throw new TokenValidationException("토큰에서 사용자 ID를 추출할 수 없습니다.");
        }
    }

    /**
     * 토큰에서 사용자 권한 추출
     *
     * @param token JWT 토큰
     * @return 사용자 권한
     */
    public UserRole getUserRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String roleString = claims.get(ROLE_CLAIM, String.class);

        if (roleString == null) {
            throw new TokenValidationException("토큰에서 사용자 권한을 추출할 수 없습니다.");
        }

        try {
            return UserRole.valueOf(roleString);
        } catch (IllegalArgumentException e) {
            throw new TokenValidationException("유효하지 않은 사용자 권한입니다: " + roleString);
        }
    }

    /**
     * 토큰에서 플랫폼 정보 추출
     *
     * @param token JWT 토큰
     * @return 플랫폼 정보 (없으면 null)
     */
    public String getPlatformFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get(PLATFORM_CLAIM, String.class);
    }

    /**
     * 토큰 유효성 검증
     *
     * @param token JWT 토큰
     * @return 유효하면 true
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);

            // 만료 시간 확인
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                log.debug("토큰이 만료되었습니다. 만료시간: {}", expiration);
                return false;
            }

            // 토큰 타입 확인 (Access Token만 유효)
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
                log.debug("유효하지 않은 토큰 타입입니다: {}", tokenType);
                return false;
            }

            // 필수 클레임 확인
            if (claims.get(USER_ID_CLAIM) == null || claims.get(ROLE_CLAIM) == null) {
                log.debug("필수 클레임이 누락되었습니다.");
                return false;
            }

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh Token 유효성 검증
     *
     * @param refreshToken Refresh Token
     * @return 유효하면 true
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = getClaimsFromToken(refreshToken);

            // 만료 시간 확인
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                log.debug("Refresh Token이 만료되었습니다. 만료시간: {}", expiration);
                return false;
            }

            // 토큰 타입 확인 (Refresh Token만 유효)
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
                log.debug("유효하지 않은 Refresh Token 타입입니다: {}", tokenType);
                return false;
            }

            // 필수 클레임 확인
            if (claims.get(USER_ID_CLAIM) == null) {
                log.debug("Refresh Token에 사용자 ID가 누락되었습니다.");
                return false;
            }

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Refresh Token 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 만료 시간 확인
     *
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public LocalDateTime getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Date expiration = claims.getExpiration();
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 토큰 발급 시간 확인
     *
     * @param token JWT 토큰
     * @return 발급 시간
     */
    public LocalDateTime getIssuedAtFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Date issuedAt = claims.getIssuedAt();
        return issuedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Access Token 유효시간 (초)
     *
     * @return 유효시간 (초)
     */
    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInMilliseconds / 1000;
    }

    /**
     * Refresh Token 유효시간 (초)
     *
     * @return 유효시간 (초)
     */
    public long getRefreshTokenValidityInSeconds() {
        return refreshTokenValidityInMilliseconds / 1000;
    }

    /**
     * 토큰에서 Claims 추출
     *
     * @param token JWT 토큰
     * @return Claims 객체
     * @throws TokenValidationException 토큰 파싱 실패 시
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            log.debug("토큰이 만료되었습니다: {}", e.getMessage());
            throw new TokenValidationException("토큰이 만료되었습니다.", e);

        } catch (UnsupportedJwtException e) {
            log.debug("지원되지 않는 토큰입니다: {}", e.getMessage());
            throw new TokenValidationException("지원되지 않는 토큰입니다.", e);

        } catch (MalformedJwtException e) {
            log.debug("잘못된 형식의 토큰입니다: {}", e.getMessage());
            throw new TokenValidationException("잘못된 형식의 토큰입니다.", e);

        } catch (SecurityException e) {
            log.debug("토큰 서명이 유효하지 않습니다: {}", e.getMessage());
            throw new TokenValidationException("토큰 서명이 유효하지 않습니다.", e);

        } catch (IllegalArgumentException e) {
            log.debug("토큰이 비어있거나 null입니다: {}", e.getMessage());
            throw new TokenValidationException("토큰이 비어있거나 null입니다.", e);
        }
    }
}