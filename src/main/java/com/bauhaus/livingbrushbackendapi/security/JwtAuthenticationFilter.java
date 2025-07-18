package com.bauhaus.livingbrushbackendapi.security;

import com.bauhaus.livingbrushbackendapi.entity.enumeration.UserRole;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 인증 필터
 *
 * 모든 HTTP 요청에서 JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정
 *
 * 주요 기능:
 * - Authorization 헤더에서 Bearer 토큰 추출
 * - JWT 토큰 유효성 검증
 * - 사용자 정보 추출 및 UserDetails 생성
 * - SecurityContext에 Authentication 객체 설정
 * - 플랫폼별 권한 관리 (VR/AR)
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // Authorization 헤더 관련 상수
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    /**
     * JWT 토큰 검증 및 인증 처리
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException I/O 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            String jwt = extractTokenFromRequest(request);

            // 2. 토큰이 있고 유효한 경우 인증 처리
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // 토큰에서 사용자 정보 추출
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                UserRole userRole = jwtTokenProvider.getUserRoleFromToken(jwt);
                String platform = jwtTokenProvider.getPlatformFromToken(jwt);

                // UserDetails 생성
                UserDetails userDetails = createUserDetails(userId, userRole, platform);

                // Authentication 객체 생성
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                );

                // SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 요청 속성에 사용자 정보 저장 (컨트롤러에서 활용 가능)
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", userRole);
                request.setAttribute("platform", platform);

                log.debug("JWT 인증 성공 - 사용자 ID: {}, 권한: {}, 플랫폼: {}, URI: {}", 
                         userId, userRole, platform, request.getRequestURI());

            } else if (StringUtils.hasText(jwt)) {
                // 토큰이 있지만 유효하지 않은 경우
                log.debug("유효하지 않은 JWT 토큰 - URI: {}", request.getRequestURI());
                SecurityContextHolder.clearContext();
            }

        } catch (Exception e) {
            // JWT 처리 중 예외 발생 시 로깅 후 인증 정보 제거
            log.error("JWT 인증 처리 중 예외 발생 - URI: {}, 에러: {}", 
                     request.getRequestURI(), e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     *
     * @param request HTTP 요청
     * @return JWT 토큰 (Bearer 접두사 제거된 상태) 또는 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX_LENGTH);
        }

        return null;
    }

    /**
     * JWT 토큰 정보로 UserDetails 생성
     *
     * @param userId 사용자 ID
     * @param userRole 사용자 권한
     * @param platform 플랫폼 정보
     * @return UserDetails 객체
     */
    private UserDetails createUserDetails(Long userId, UserRole userRole, String platform) {
        // Spring Security 권한 생성
        List<GrantedAuthority> authorities = createAuthorities(userRole, platform);

        // 사용자명은 userId 사용 (실제 username이 없으므로)
        String username = "user_" + userId;

        return User.builder()
                .username(username)
                .password("") // JWT 기반 인증이므로 비밀번호 불필요
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * 사용자 권한과 플랫폼에 따른 GrantedAuthority 생성
     *
     * @param userRole 사용자 권한
     * @param platform 플랫폼 정보
     * @return GrantedAuthority 리스트
     */
    private List<GrantedAuthority> createAuthorities(UserRole userRole, String platform) {
        // 기본 역할 권한
        String roleAuthority = "ROLE_" + userRole.name();
        
        // 플랫폼별 추가 권한 (필요 시)
        if ("vr".equalsIgnoreCase(platform)) {
            // VR 앱은 아티스트 권한이 있어야 작품 생성 가능
            if (userRole == UserRole.ARTIST || userRole == UserRole.ADMIN) {
                return List.of(
                    new SimpleGrantedAuthority(roleAuthority),
                    new SimpleGrantedAuthority("PLATFORM_VR"),
                    new SimpleGrantedAuthority("CAN_CREATE_ARTWORK")
                );
            }
        } else if ("ar".equalsIgnoreCase(platform)) {
            // AR 앱은 기본적으로 관람객 권한
            return List.of(
                new SimpleGrantedAuthority(roleAuthority),
                new SimpleGrantedAuthority("PLATFORM_AR"),
                new SimpleGrantedAuthority("CAN_VIEW_ARTWORK")
            );
        }

        // 기본 권한만 반환
        return Collections.singletonList(new SimpleGrantedAuthority(roleAuthority));
    }

    /**
     * 특정 요청에 대해 필터를 적용하지 않을지 결정
     *
     * @param request HTTP 요청
     * @return 필터를 건너뛸 경우 true
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS 요청은 필터 건너뛰기 (CORS 프리플라이트)
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // 공개 엔드포인트는 필터 건너뛰기
        return isPublicEndpoint(path);
    }

    /**
     * 공개 엔드포인트 여부 확인
     *
     * @param path 요청 경로
     * @return 공개 엔드포인트면 true
     */
    private boolean isPublicEndpoint(String path) {
        // 인증이 필요 없는 공개 API 경로들
        String[] publicPaths = {
            "/api/auth/",           // 인증 관련 API
            "/login/oauth2/",       // OAuth2 리다이렉트
            "/oauth2/",             // OAuth2 관련
            "/api/artworks/public/", // 공개 작품 조회
            "/api/qr/",             // QR 코드 스캔
            "/health",              // 헬스체크
            "/actuator/",           // 액추에이터
            "/swagger-ui/",         // Swagger UI
            "/v3/api-docs/",        // API 문서
            "/favicon.ico"          // 파비콘
        };

        for (String publicPath : publicPaths) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }

        return false;
    }
}
