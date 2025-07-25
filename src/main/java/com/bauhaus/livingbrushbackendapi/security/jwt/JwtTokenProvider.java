package com.bauhaus.livingbrushbackendapi.security.jwt;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String PLATFORM_CLAIM = "platform"; // VR 전용 클레임
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String VR_PLATFORM = "VR"; // VR 플랫폼 식별자

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") Duration accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") Duration refreshTokenValidity
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMilliseconds = accessTokenValidity.toMillis();
        this.refreshTokenValidityInMilliseconds = refreshTokenValidity.toMillis();
        log.info("JWT Provider initialized. Access token validity: {}ms, Refresh token validity: {}ms",
                this.accessTokenValidityInMilliseconds, this.refreshTokenValidityInMilliseconds);
    }

    public String createAccessToken(Long userId, UserRole userRole) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(ROLE_CLAIM, userRole.name());
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        return generateToken(userId.toString(), accessTokenValidityInMilliseconds, claims);
    }

    public String createRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        return generateToken(userId.toString(), refreshTokenValidityInMilliseconds, claims);
    }

    /**
     * VR 환경에 최적화된 액세스 토큰을 생성합니다.
     * 
     * 기존 createAccessToken과 동일한 구조이지만, VR 플랫폼 식별자를 추가로 포함합니다.
     * 향후 VR 전용 기능이나 권한 관리가 필요할 때 활용할 수 있습니다.
     * 
     * @param userId 사용자 ID
     * @param userRole 사용자 권한
     * @return VR 전용 JWT 액세스 토큰
     */
    public String createVrAccessToken(Long userId, UserRole userRole) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(ROLE_CLAIM, userRole.name());
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        claims.put(PLATFORM_CLAIM, VR_PLATFORM); // VR 플랫폼 식별자 추가
        
        log.debug("VR 액세스 토큰 생성 - User ID: {}, Role: {}", userId, userRole);
        return generateToken(userId.toString(), accessTokenValidityInMilliseconds, claims);
    }

    public Long getUserIdFromToken(String token) {
        String subject = getClaimsFromToken(token).getSubject();
        return Long.parseLong(subject);
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = getClaimsFromToken(accessToken);

        if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Access Token이 아닙니다.");
        }

        Long userId = Long.parseLong(claims.getSubject());
        String roleString = claims.get(ROLE_CLAIM, String.class);
        if (roleString == null) {
            throw new CustomException(ErrorCode.MISSING_CLAIMS, "토큰에 권한 정보가 없습니다.");
        }

        UserRole role = UserRole.valueOf(roleString);
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());

        return new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(authority));
    }

    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = getClaimsFromToken(refreshToken);
            return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
        } catch (CustomException e) {
            log.warn("Refresh Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Spring Security의 Authentication 객체에서 사용자 ID를 안전하게 추출합니다.
     * 컨트롤러에서 인증된 사용자의 ID를 쉽게 가져올 수 있도록 돕는 편의 메소드입니다.
     *
     * @param authentication SecurityContext에 저장된 인증 객체
     * @return 사용자 ID (Long)
     */
    public Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            log.warn("getUserIdFromAuthentication - Authentication principal is not a Long. Principal: {}",
                    authentication != null ? authentication.getPrincipal() : "null");
            throw new CustomException(ErrorCode.ACCESS_DENIED, "유효한 사용자 인증 정보가 없습니다.");
        }
        return (Long) authentication.getPrincipal();
    }

    /**
     * 토큰에서 플랫폼 정보를 추출합니다.
     * 
     * VR 전용 토큰인지 확인하거나, 플랫폼별 로직 분기에 활용할 수 있습니다.
     * 
     * @param token JWT 토큰
     * @return 플랫폼 식별자 (VR, 또는 null)
     */
    public String getPlatformFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get(PLATFORM_CLAIM, String.class);
        } catch (CustomException e) {
            log.debug("토큰에서 플랫폼 정보 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * VR 플랫폼에서 생성된 토큰인지 확인합니다.
     * 
     * @param token JWT 토큰
     * @return VR 토큰이면 true, 아니면 false
     */
    public boolean isVrToken(String token) {
        String platform = getPlatformFromToken(token);
        return VR_PLATFORM.equals(platform);
    }

    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(ErrorCode.UNSUPPORTED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new CustomException(ErrorCode.MALFORMED_TOKEN);
        } catch (SecurityException e) {
            throw new CustomException(ErrorCode.INVALID_SIGNATURE);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "토큰이 비어있거나 null입니다.");
        }
    }

    private String generateToken(String subject, long validityMillis, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }
}