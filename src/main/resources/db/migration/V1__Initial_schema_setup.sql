-- ====================================================================
-- BAUhaus 통합 계정 지원 데이터베이스 구축 스크립트 (V1.2 최종 완성본)
--
-- 변경 사항 (v1.2):
-- - 모든 테이블에 created_at, updated_at 컬럼 추가하여 BaseEntity와 완벽 동기화
-- - 모든 테이블에 updated_at 자동 갱신 트리거 적용하여 데이터 정합성 보장
--
-- 특징:
-- - 다중 OAuth 계정 연동 지원 (Meta + Google + Facebook)
-- - ENUM 타입 사용으로 타입 안전성 및 성능 최적화
-- - Hibernate와 완벽 호환 (ddl-auto: validate 모드)
-- - VR→AR, AR→VR 모든 가입 시나리오 지원
-- - 클린 아키텍처 원칙 준수
-- ====================================================================

-- ========== 확장 모듈 활성화 ==========
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;

-- ========== ENUM 타입 정의 (타입 안전성 확보) ==========
DO $$ BEGIN
    CREATE TYPE userrole AS ENUM ('GUEST', 'USER', 'ARTIST', 'ADMIN');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE usermode AS ENUM ('AR', 'ARTIST');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE visibilitytype AS ENUM ('PRIVATE', 'PUBLIC');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE mediatype AS ENUM ('AUDIO', 'IMAGE', 'MODEL_3D', 'VIDEO');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE airequesttype AS ENUM ('BRUSH', 'PALETTE', 'CHATBOT');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ========== 공통 트리거 함수 ==========
CREATE OR REPLACE FUNCTION update_updated_at_column() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ========== 핵심 테이블 생성 ==========

-- 1. users: 통합 계정 사용자 정보
CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       nickname VARCHAR(50) NOT NULL,
                       email VARCHAR(255),
                       meta_user_id VARCHAR(255),
                       google_user_id VARCHAR(255),
                       facebook_user_id VARCHAR(255),
                       primary_provider VARCHAR(20) NOT NULL,
                       role userrole NOT NULL DEFAULT 'GUEST',
                       highest_role userrole NOT NULL DEFAULT 'GUEST',
                       current_mode usermode NOT NULL DEFAULT 'AR',
                       account_linked BOOLEAN NOT NULL DEFAULT FALSE,
                       artist_qualified_at TIMESTAMP WITH TIME ZONE,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       CONSTRAINT users_nickname_length_check CHECK (LENGTH(nickname) BETWEEN 2 AND 50),
                       CONSTRAINT users_primary_provider_check CHECK (primary_provider IN ('META', 'GOOGLE', 'FACEBOOK')),
                       CONSTRAINT users_meta_user_id_uk UNIQUE (meta_user_id),
                       CONSTRAINT users_google_user_id_uk UNIQUE (google_user_id),
                       CONSTRAINT users_facebook_user_id_uk UNIQUE (facebook_user_id),
                       CONSTRAINT users_oauth_required CHECK (meta_user_id IS NOT NULL OR google_user_id IS NOT NULL OR facebook_user_id IS NOT NULL),
                       CONSTRAINT users_role_hierarchy CHECK ((role = 'GUEST') OR (role = 'USER' AND highest_role IN ('USER', 'ARTIST', 'ADMIN')) OR (role = 'ARTIST' AND highest_role IN ('ARTIST', 'ADMIN')) OR (role = 'ADMIN'))
);

-- 2. user_settings: AI 동의 및 설정
CREATE TABLE user_settings (
                               user_id BIGINT PRIMARY KEY,
                               stt_consent BOOLEAN NOT NULL DEFAULT FALSE,
                               ai_consent BOOLEAN NOT NULL DEFAULT FALSE,
                               data_training_consent BOOLEAN NOT NULL DEFAULT FALSE,
                               custom_settings JSONB DEFAULT '{}'::jsonb,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                               CONSTRAINT user_settings_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3. artworks: 3D 작품 정보
CREATE TABLE artworks (
                          artwork_id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          title VARCHAR(255) NOT NULL,
                          description TEXT,
                          glb_url VARCHAR(2048) NOT NULL,
                          thumbnail_media_id BIGINT,
                          visibility visibilitytype NOT NULL DEFAULT 'PRIVATE',
                          price_cash DECIMAL(10,2),
                          favorite_count INT NOT NULL DEFAULT 0,
                          view_count INT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT artworks_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                          CONSTRAINT artworks_title_length_check CHECK (LENGTH(title) BETWEEN 1 AND 255),
                          CONSTRAINT artworks_description_length_check CHECK (LENGTH(description) <= 1000),
                          CONSTRAINT artworks_price_cash_range_check CHECK (price_cash >= 0 AND price_cash <= 100),
                          CONSTRAINT artworks_favorite_count_positive_check CHECK (favorite_count >= 0),
                          CONSTRAINT artworks_view_count_positive_check CHECK (view_count >= 0)
);

-- 4. media: 스크린샷/GIF/영상
CREATE TABLE media (
                       media_id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       artwork_id BIGINT,
                       media_type mediatype NOT NULL,
                       file_url VARCHAR(2048) NOT NULL,
                       duration_seconds INT,
                       thumbnail_url VARCHAR(2048),
                       visibility visibilitytype NOT NULL DEFAULT 'PRIVATE',
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       CONSTRAINT media_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                       CONSTRAINT media_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE SET NULL,
                       CONSTRAINT media_duration_seconds_check CHECK ((media_type = 'IMAGE' AND duration_seconds IS NULL) OR (media_type IN ('AUDIO', 'VIDEO') AND duration_seconds <= 600))
);

ALTER TABLE artworks ADD CONSTRAINT artworks_thumbnail_media_id_fk FOREIGN KEY (thumbnail_media_id) REFERENCES media(media_id) ON DELETE SET NULL;

-- 5. tags: 태그 마스터
CREATE TABLE tags (
                      tag_id BIGSERIAL PRIMARY KEY,
                      tag_name VARCHAR(50) NOT NULL,
                      usage_count INT NOT NULL DEFAULT 0,
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                      CONSTRAINT tags_tag_name_uk UNIQUE (tag_name),
                      CONSTRAINT tags_tag_name_length_check CHECK (LENGTH(tag_name) BETWEEN 1 AND 50),
                      CONSTRAINT tags_usage_count_positive_check CHECK (usage_count >= 0)
);

-- 6. artwork_tags: 작품-태그 관계
CREATE TABLE artwork_tags (
                              artwork_id BIGINT,
                              tag_id BIGINT,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                              PRIMARY KEY (artwork_id, tag_id),
                              CONSTRAINT artwork_tags_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE,
                              CONSTRAINT artwork_tags_tag_id_fk FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

-- 7. qr_codes: QR 코드 관리
CREATE TABLE qr_codes (
                          qr_id BIGSERIAL PRIMARY KEY,
                          artwork_id BIGINT NOT NULL,
                          qr_token UUID NOT NULL DEFAULT gen_random_uuid(),
                          qr_image_url VARCHAR(2048),
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                          CONSTRAINT qr_codes_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE,
                          CONSTRAINT qr_codes_qr_token_uk UNIQUE (qr_token)
);

-- 8. qr_scan_history: QR 스캔 이력
CREATE TABLE qr_scan_history (
                                 scan_id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT,
                                 qr_id BIGINT NOT NULL,
                                 scanned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                                 updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                                 CONSTRAINT qr_scan_history_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
                                 CONSTRAINT qr_scan_history_qr_id_fk FOREIGN KEY (qr_id) REFERENCES qr_codes(qr_id) ON DELETE CASCADE
);

-- 9. location_exhibitions: 장소 기반 전시
CREATE TABLE location_exhibitions (
                                      exhibition_id BIGSERIAL PRIMARY KEY,
                                      artwork_id BIGINT NOT NULL,
                                      location_name VARCHAR(255) NOT NULL,
                                      location_point GEOGRAPHY(Point, 4326) NOT NULL,
                                      radius_meters INT NOT NULL,
                                      is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                      start_date TIMESTAMP WITH TIME ZONE,
                                      end_date TIMESTAMP WITH TIME ZONE,
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                      CONSTRAINT location_exhibitions_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE,
                                      CONSTRAINT location_exhibitions_location_name_length_check CHECK (LENGTH(location_name) BETWEEN 1 AND 255),
                                      CONSTRAINT location_exhibitions_radius_meters_range_check CHECK (radius_meters > 0 AND radius_meters <= 1000),
                                      CONSTRAINT location_exhibitions_date_order_check CHECK (end_date IS NULL OR start_date < end_date)
);

-- 10. ai_generated_assets: AI 생성 에셋 관리
CREATE TABLE ai_generated_assets (
                                     asset_id BIGSERIAL PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     asset_type VARCHAR(20) NOT NULL,
                                     asset_data JSONB NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                     updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                                     CONSTRAINT ai_generated_assets_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                     CONSTRAINT ai_generated_assets_asset_type_enum_check CHECK (asset_type IN ('BRUSH', 'PALETTE'))
);

-- 11. ai_request_logs: AI 요청 로깅
CREATE TABLE ai_request_logs (
                                 log_id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 request_type airequesttype NOT NULL,
                                 request_text TEXT,
                                 response_data JSONB,
                                 is_success BOOLEAN NOT NULL DEFAULT FALSE,
                                 error_code VARCHAR(50),
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                 updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                 CONSTRAINT ai_request_logs_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 12. notifications: 알림 시스템
CREATE TABLE notifications (
                               notification_id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               notification_type VARCHAR(50) NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               message TEXT NOT NULL,
                               related_id BIGINT,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                               CONSTRAINT notifications_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                               CONSTRAINT notifications_title_length_check CHECK (LENGTH(title) BETWEEN 1 AND 255),
                               CONSTRAINT notifications_message_length_check CHECK (LENGTH(message) <= 500)
);

-- 13. user_profiles: 프로필 관리
CREATE TABLE user_profiles (
                               user_id BIGINT PRIMARY KEY,
                               profile_image_url VARCHAR(2048),
                               bio TEXT,
                               bio_public BOOLEAN NOT NULL DEFAULT TRUE,
                               join_date_public BOOLEAN NOT NULL DEFAULT TRUE,
                               follower_count INT NOT NULL DEFAULT 0,
                               following_count INT NOT NULL DEFAULT 0,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                               CONSTRAINT user_profiles_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                               CONSTRAINT user_profiles_bio_length_check CHECK (LENGTH(bio) <= 100),
                               CONSTRAINT user_profiles_follower_count_check CHECK (follower_count >= 0),
                               CONSTRAINT user_profiles_following_count_check CHECK (following_count >= 0)
);

-- 14. favorite_artworks: 작품 즐겨찾기
CREATE TABLE favorite_artworks (
                                   user_id BIGINT,
                                   artwork_id BIGINT,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                                   PRIMARY KEY (user_id, artwork_id),
                                   CONSTRAINT favorite_artworks_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                   CONSTRAINT favorite_artworks_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE
);

-- 15. follows: 아티스트 팔로우
CREATE TABLE follows (
                         follower_id BIGINT,
                         following_id BIGINT,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                         PRIMARY KEY (follower_id, following_id),
                         CONSTRAINT follows_follower_id_fk FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
                         CONSTRAINT follows_following_id_fk FOREIGN KEY (following_id) REFERENCES users(user_id) ON DELETE CASCADE,
                         CONSTRAINT follows_self_follow_check CHECK (follower_id != following_id)
);

-- 16. media_tags: 미디어-태그 관계
CREATE TABLE media_tags (
                            media_id BIGINT,
                            tag_id BIGINT,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                            PRIMARY KEY (media_id, tag_id),
                            CONSTRAINT media_tags_media_id_fk FOREIGN KEY (media_id) REFERENCES media(media_id) ON DELETE CASCADE,
                            CONSTRAINT media_tags_tag_id_fk FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

-- 17. likes: 작품 좋아요
CREATE TABLE likes (
                       like_id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       artwork_id BIGINT NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                       CONSTRAINT likes_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                       CONSTRAINT likes_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE,
                       CONSTRAINT likes_user_artwork_uk UNIQUE(user_id, artwork_id)
);

-- 18. comments: 작품 댓글
CREATE TABLE comments (
                          comment_id BIGSERIAL PRIMARY KEY,
                          artwork_id BIGINT NOT NULL,
                          user_id BIGINT NOT NULL,
                          content TEXT NOT NULL,
                          is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                          CONSTRAINT comments_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE,
                          CONSTRAINT comments_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                          CONSTRAINT comments_content_length_check CHECK (LENGTH(content) BETWEEN 1 AND 200)
);

-- 19. account_linking_history: 계정 연동 이력 추적
CREATE TABLE account_linking_history (
                                         history_id BIGSERIAL PRIMARY KEY,
                                         user_id BIGINT NOT NULL,
                                         action_type VARCHAR(20) NOT NULL,
                                         provider VARCHAR(20) NOT NULL,
                                         provider_user_id VARCHAR(255) NOT NULL,
                                         previous_role userrole,
                                         new_role userrole,
                                         linked_from_user_id BIGINT,
                                         created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                                         CONSTRAINT account_linking_history_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                         CONSTRAINT account_linking_history_action_check CHECK (action_type IN ('CREATED', 'LINKED', 'PROMOTED', 'MERGED', 'UNLINKED')),
                                         CONSTRAINT account_linking_history_provider_check CHECK (provider IN ('META', 'GOOGLE', 'FACEBOOK'))
);

-- 20. user_preferences: 사용자 추천 설정
CREATE TABLE user_preferences (
                                  user_id BIGINT PRIMARY KEY,
                                  preferred_tags BIGINT[] DEFAULT '{}',
                                  preferred_artists BIGINT[] DEFAULT '{}',
                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- [추가]
                                  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                  CONSTRAINT user_preferences_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 21. account_pairings: VR-AR 계정 페어링 관리
CREATE TABLE account_pairings (
    pairing_id BIGSERIAL PRIMARY KEY,
    pairing_code UUID NOT NULL DEFAULT gen_random_uuid(),
    ar_user_id BIGINT NOT NULL,
    qr_image_url VARCHAR(2048),
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    linked_meta_user_id VARCHAR(255),
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- 제약 조건
    CONSTRAINT account_pairings_ar_user_id_fk 
        FOREIGN KEY (ar_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT account_pairings_pairing_code_uk 
        UNIQUE (pairing_code),
    CONSTRAINT account_pairings_expires_at_check 
        CHECK (expires_at > created_at),
    CONSTRAINT account_pairings_completion_check 
        CHECK ((is_used = TRUE AND linked_meta_user_id IS NOT NULL AND completed_at IS NOT NULL) 
               OR (is_used = FALSE AND linked_meta_user_id IS NULL AND completed_at IS NULL))
);

-- ========== 트리거 생성 (모든 테이블에 적용) ==========
-- [수정] 모든 테이블에 updated_at 자동 갱신 트리거를 적용하여 일관성 확보
CREATE TRIGGER trigger_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_user_settings_updated_at BEFORE UPDATE ON user_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_artworks_updated_at BEFORE UPDATE ON artworks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_media_updated_at BEFORE UPDATE ON media FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_tags_updated_at BEFORE UPDATE ON tags FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_artwork_tags_updated_at BEFORE UPDATE ON artwork_tags FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_qr_codes_updated_at BEFORE UPDATE ON qr_codes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_qr_scan_history_updated_at BEFORE UPDATE ON qr_scan_history FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_location_exhibitions_updated_at BEFORE UPDATE ON location_exhibitions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_ai_generated_assets_updated_at BEFORE UPDATE ON ai_generated_assets FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_ai_request_logs_updated_at BEFORE UPDATE ON ai_request_logs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_notifications_updated_at BEFORE UPDATE ON notifications FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_user_profiles_updated_at BEFORE UPDATE ON user_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_favorite_artworks_updated_at BEFORE UPDATE ON favorite_artworks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_follows_updated_at BEFORE UPDATE ON follows FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_media_tags_updated_at BEFORE UPDATE ON media_tags FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_likes_updated_at BEFORE UPDATE ON likes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_comments_updated_at BEFORE UPDATE ON comments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_account_linking_history_updated_at BEFORE UPDATE ON account_linking_history FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_user_preferences_updated_at BEFORE UPDATE ON user_preferences FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_account_pairings_updated_at BEFORE UPDATE ON account_pairings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ========== 최적화 인덱스 생성 ==========
CREATE INDEX users_meta_user_id_idx ON users(meta_user_id) WHERE meta_user_id IS NOT NULL;
CREATE INDEX users_google_user_id_idx ON users(google_user_id) WHERE google_user_id IS NOT NULL;
CREATE INDEX users_facebook_user_id_idx ON users(facebook_user_id) WHERE facebook_user_id IS NOT NULL;
CREATE INDEX users_primary_provider_idx ON users(primary_provider);
CREATE INDEX users_role_idx ON users(role);
CREATE INDEX users_artist_qualified_idx ON users(artist_qualified_at) WHERE artist_qualified_at IS NOT NULL;
CREATE INDEX users_account_linked_idx ON users(account_linked) WHERE account_linked = true;
CREATE INDEX users_email_idx ON users(email);
CREATE INDEX artworks_user_id_idx ON artworks(user_id);
CREATE INDEX artworks_visibility_idx ON artworks(visibility);
CREATE INDEX media_artwork_id_idx ON media(artwork_id);
CREATE INDEX ai_request_logs_user_id_request_type_idx ON ai_request_logs(user_id, request_type);
CREATE INDEX comments_artwork_id_user_id_idx ON comments(artwork_id, user_id);
CREATE INDEX notifications_user_id_idx ON notifications(user_id);
CREATE INDEX location_exhibitions_location_point_gist ON location_exhibitions USING GIST (location_point);
CREATE INDEX account_linking_history_user_id_idx ON account_linking_history(user_id);
CREATE INDEX account_linking_history_action_type_idx ON account_linking_history(action_type, created_at);
CREATE INDEX account_linking_history_provider_idx ON account_linking_history(provider, provider_user_id);
CREATE INDEX account_pairings_pairing_code_idx ON account_pairings(pairing_code);
CREATE INDEX account_pairings_expires_at_idx ON account_pairings(expires_at);
CREATE INDEX account_pairings_ar_user_id_idx ON account_pairings(ar_user_id);
CREATE INDEX account_pairings_is_used_idx ON account_pairings(is_used) WHERE is_used = FALSE;
CREATE INDEX account_pairings_linked_meta_user_id_idx ON account_pairings(linked_meta_user_id) WHERE linked_meta_user_id IS NOT NULL;

-- ========== 기본 데이터 삽입 ==========
INSERT INTO tags (tag_name) VALUES
                                ('추상화'), ('풍경화'), ('인물화'), ('정물화'), ('동물'),
                                ('건축물'), ('자연'), ('도시'), ('바다'), ('산'),
                                ('꽃'), ('나무'), ('구름'), ('일몰'), ('밤하늘'),
                                ('미니멀'), ('컬러풀'), ('모노크롬'), ('비현실적'), ('환상적'),
                                ('따뜻한'), ('차가운'), ('밝은'), ('어두운'), ('신비로운'),
                                ('평화로운'), ('역동적'), ('고요한'), ('화려한'), ('단순한')
ON CONFLICT (tag_name) DO NOTHING;

DO $$
    DECLARE
        v_test_user_id BIGINT;
    BEGIN
        INSERT INTO users (nickname, email, primary_provider, meta_user_id, role, highest_role, current_mode, artist_qualified_at)
        VALUES ('test_artist', 'test@bauhaus.com', 'META', 'test12345', 'ARTIST', 'ARTIST', 'ARTIST', NOW())
        ON CONFLICT (meta_user_id) DO UPDATE SET
                                                 nickname = EXCLUDED.nickname,
                                                 email = EXCLUDED.email
        RETURNING user_id INTO v_test_user_id;

        IF v_test_user_id IS NOT NULL THEN
            INSERT INTO account_linking_history (user_id, action_type, provider, provider_user_id, previous_role, new_role)
            VALUES (v_test_user_id, 'CREATED', 'META', 'test12345', NULL, 'ARTIST')
            ON CONFLICT DO NOTHING;

            INSERT INTO user_settings (user_id, stt_consent, ai_consent, data_training_consent)
            VALUES (v_test_user_id, true, true, true)
            ON CONFLICT (user_id) DO NOTHING;

            INSERT INTO user_profiles (user_id, profile_image_url, bio, bio_public, join_date_public)
            VALUES (v_test_user_id, 'https://livingbrush-storage.s3.ap-northeast-2.amazonaws.com/profiles/user-1/profile_750e2cc34b88456b94f17023234459a2.png', '안녕하세요! VR로 3D 아트를 창작하는 아티스트입니다. 가상현실에서 그린 작품들을 AR로 현실에 가져와 새로운 예술 경험을 만들어가고 있어요. 🎨✨', true, true)
            ON CONFLICT (user_id) DO UPDATE SET
                profile_image_url = EXCLUDED.profile_image_url,
                bio = EXCLUDED.bio;

            INSERT INTO artworks (user_id, title, description, glb_url, visibility)
            VALUES (v_test_user_id, 'VR 샘플 작품', 'QR 테스트용 샘플 작품입니다', 'https://livingbrush-storage.s3.ap-northeast-2.amazonaws.com/artworks/user-1/artwork-1/models/sample.glb', 'PUBLIC')
            ON CONFLICT DO NOTHING;
        END IF;
    END
$$;