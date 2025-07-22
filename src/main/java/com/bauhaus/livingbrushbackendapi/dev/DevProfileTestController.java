package com.bauhaus.livingbrushbackendapi.dev;

import com.bauhaus.livingbrushbackendapi.user.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 개발 환경 전용 - 프로필 이미지 업로드 테스트 컨트롤러
 * 
 * 실제 OAuth 없이 ProfileImageService를 테스트할 수 있습니다.
 */
@Slf4j
@RestController
@RequestMapping("/dev/profile")
@RequiredArgsConstructor
// @Profile 제거 - 모든 환경에서 테스트 가능
public class DevProfileTestController {

    private final ProfileImageService profileImageService;

    /**
     * 프로필 이미지 URL 테스트
     * 
     * 실제 OAuth 제공자의 프로필 이미지 URL을 시뮬레이션하여 S3 업로드를 테스트합니다.
     */
    @PostMapping("/test-upload")
    public ResponseEntity<Map<String, Object>> testProfileImageUpload(
            @RequestParam Long userId,
            @RequestParam String imageUrl) {
        
        try {
            log.info("프로필 이미지 업로드 테스트 시작 - 사용자 ID: {}, 이미지 URL: {}", userId, imageUrl);
            
            // ProfileImageService를 통해 실제 업로드 수행
            String uploadedUrl = profileImageService.uploadProfileImageFromUrl(userId, imageUrl);
            
            log.info("프로필 이미지 업로드 테스트 완료 - 업로드된 URL: {}", uploadedUrl);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "프로필 이미지 업로드 성공",
                "userId", userId,
                "originalUrl", imageUrl,
                "uploadedUrl", uploadedUrl
            ));
            
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 테스트 실패: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "프로필 이미지 업로드 실패: " + e.getMessage(),
                "userId", userId,
                "originalUrl", imageUrl,
                "uploadedUrl", profileImageService.getDefaultProfileImageUrl()
            ));
        }
    }

    /**
     * 기본 프로필 이미지 URL 조회
     */
    @GetMapping("/default-image")
    public ResponseEntity<Map<String, String>> getDefaultProfileImage() {
        String defaultUrl = profileImageService.getDefaultProfileImageUrl();
        
        return ResponseEntity.ok(Map.of(
            "defaultProfileImageUrl", defaultUrl
        ));
    }

    /**
     * 프로필 이미지 서비스 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        return ResponseEntity.ok(Map.of(
            "service", "ProfileImageService",
            "status", "active",
            "description", "OAuth 프로필 이미지를 S3에 업로드하는 서비스",
            "testEndpoint", "/dev/profile/test-upload?userId=1&imageUrl=https://example.com/image.jpg"
        ));
    }
}
