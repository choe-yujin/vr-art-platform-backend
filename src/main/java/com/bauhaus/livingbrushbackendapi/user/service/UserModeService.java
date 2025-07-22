package com.bauhaus.livingbrushbackendapi.user.service;

import com.bauhaus.livingbrushbackendapi.auth.service.UserPermissionService;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.dto.UserModeResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.UserPermissionResponse;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserMode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 모드 전환 서비스
 * 
 * 정책: 아티스트 자격을 가진 사용자만 아티스트↔관람객 모드 전환 가능
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserModeService {

    private final UserRepository userRepository;
    private final UserPermissionService userPermissionService;

    /**
     * 사용자 모드를 전환합니다.
     * 
     * @param userId 사용자 ID
     * @param newMode 전환할 모드
     * @param reason 전환 사유
     * @return 모드 전환 결과
     */
    @Transactional
    public UserModeResponse switchMode(Long userId, UserMode newMode, String reason) {
        log.info("🔄 모드 전환 요청 - 사용자 ID: {}, 새 모드: {}, 사유: {}", userId, newMode, reason);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // 현재 모드와 동일한 경우
        if (user.getCurrentMode() == newMode) {
            log.info("ℹ️ 이미 동일한 모드입니다 - 사용자 ID: {}, 모드: {}", userId, newMode);
            return UserModeResponse.of(userId, newMode, user.getRole(), canSwitchMode(user));
        }
        
        // 모드 전환 권한 확인
        validateModeSwitchPermission(user, newMode);
        
        // 모드 전환 실행
        UserMode previousMode = user.getCurrentMode();
        user.setCurrentMode(newMode);
        userRepository.save(user);
        
        log.info("✅ 모드 전환 완료 - 사용자 ID: {}, {} → {}", userId, previousMode, newMode);
        
        return UserModeResponse.of(userId, newMode, user.getRole(), canSwitchMode(user));
    }

    /**
     * 사용자의 현재 권한 정보를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 권한 정보
     */
    public UserPermissionResponse getCurrentPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        boolean isArtistQualified = userPermissionService.isArtistQualified(user);
        boolean canSwitchMode = canSwitchMode(user);
        List<String> availableFeatures = getAvailableFeatures(user);
        
        return UserPermissionResponse.of(
                userId,
                user.getRole(),
                user.getCurrentMode(),
                isArtistQualified,
                user.isAccountLinked(),
                canSwitchMode,
                availableFeatures
        );
    }

    /**
     * 모드 전환 권한을 검증합니다.
     */
    private void validateModeSwitchPermission(User user, UserMode newMode) {
        // 아티스트 모드로 전환하려는 경우 - 아티스트 자격 필요
        if (newMode == UserMode.ARTIST && !userPermissionService.isArtistQualified(user)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, 
                    "아티스트 모드로 전환하려면 VR 계정 연동이 필요합니다.");
        }
        
        // 관람객(AR) 모드는 모든 사용자가 전환 가능
        log.debug("✅ 모드 전환 권한 확인 완료 - 사용자 ID: {}, 새 모드: {}", user.getUserId(), newMode);
    }

    /**
     * 모드 전환 가능 여부를 확인합니다.
     */
    private boolean canSwitchMode(User user) {
        // 아티스트 자격이 있는 사용자만 모드 전환 가능
        return userPermissionService.isArtistQualified(user);
    }

    /**
     * 사용자가 사용할 수 있는 기능 목록을 반환합니다.
     */
    private List<String> getAvailableFeatures(User user) {
        List<String> features = new ArrayList<>();
        
        // 기본 기능 (모든 사용자)
        features.add("작품 감상");
        features.add("QR 스캔");
        
        // 인증된 사용자 기능
        if (userPermissionService.isAuthenticated(user)) {
            features.add("좋아요");
            features.add("댓글 작성");
            
            if (user.getCurrentMode() == UserMode.AR) {
                features.add("팔로우");
                features.add("프로필 편집");
            }
        }
        
        // 아티스트 기능
        if (userPermissionService.isArtistQualified(user)) {
            features.add("모드 전환");
            
            if (user.getCurrentMode() == UserMode.ARTIST) {
                features.add("VR 창작");
                features.add("작품 업로드");
                features.add("AI 브러시 생성");
                features.add("AI 팔레트 생성");
                features.add("AI 챗봇");
                
                // AI 동의 여부 확인
                if (user.getUserSettings() != null && 
                    user.getUserSettings().isSttConsent() && 
                    user.getUserSettings().isAiConsent()) {
                    features.add("AI 기능 사용 가능");
                } else {
                    features.add("AI 동의 필요");
                }
            }
        }
        
        // 관리자 기능
        if (userPermissionService.isAdmin(user)) {
            features.add("관리자 패널");
            features.add("사용자 관리");
            features.add("작품 관리");
        }
        
        return features;
    }
}
