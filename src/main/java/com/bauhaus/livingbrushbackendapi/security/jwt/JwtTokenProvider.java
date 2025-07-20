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
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

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

    // ✨ 이 메소드를 추가해주세요.
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