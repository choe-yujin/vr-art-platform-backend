-- ====================================================================
-- BAUhaus 최종 데이터베이스 구축 스크립트 (JPA Entity 100% 호환)
--
-- v1.5 개선 사항:
-- - Docker 환경과의 호환성을 위해 불필요한 'postgres' 역할에 대한
--   GRANT 구문 제거
-- ====================================================================

-- (안정성 강화) 스키마가 없을 경우에만 생성합니다.
CREATE SCHEMA IF NOT EXISTS public;
-- GRANT ALL ON SCHEMA public TO postgres; -- THIS LINE IS REMOVED
GRANT ALL ON SCHEMA public TO public;

-- ========== 확장 모듈 활성화 ==========
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- UUID 생성을 위해 필수
CREATE EXTENSION IF NOT EXISTS postgis;  -- 위치 기반 서비스를 위해 PostGIS 활성화

-- ========== ENUM 타입 정의 ==========
CREATE TYPE visibility_type AS ENUM ('private', 'public');
CREATE TYPE media_type AS ENUM ('image', 'gif', 'video');
CREATE TYPE ai_request_type AS ENUM ('brush', 'palette', 'chatbot');

-- ========== 공통 트리거 함수 ==========
-- updated_at 컬럼을 자동으로 갱신하는 함수
CREATE OR REPLACE FUNCTION update_updated_at_column() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ========== 테이블 생성 (모두 소문자) ==========

-- 1. users: OAuth 사용자 정보
CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       nickname VARCHAR(50) NOT NULL,
                       email VARCHAR(255),
                       provider VARCHAR(20) NOT NULL,
                       provider_user_id VARCHAR(255) NOT NULL,
                       role VARCHAR(10) NOT NULL DEFAULT 'visitor',
                       current_mode VARCHAR(10) DEFAULT 'visitor',
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       CONSTRAINT users_nickname_len_check CHECK (LENGTH(nickname) BETWEEN 2 AND 50),
                       CONSTRAINT users_provider_check CHECK (provider IN ('meta', 'google')),
                       CONSTRAINT users_role_check CHECK (role IN ('artist', 'visitor')),
                       CONSTRAINT users_current_mode_check CHECK (current_mode IN ('artist', 'visitor')),
                       CONSTRAINT users_provider_user_id_uk UNIQUE (provider, provider_user_id)
);

-- 2. user_settings: AI 동의 및 설정
CREATE TABLE user_settings (
                               user_id BIGINT PRIMARY KEY,
                               stt_consent BOOLEAN NOT NULL DEFAULT FALSE,
                               ai_consent BOOLEAN NOT NULL DEFAULT FALSE,
                               data_training_consent BOOLEAN NOT NULL DEFAULT FALSE,
                               custom_settings JSONB DEFAULT '{}',
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
                          tilt_url VARCHAR(2048),
                          thumbnail_media_id BIGINT, -- 외래 키 제약은 media 테이블 생성 후 추가
                          visibility visibility_type NOT NULL DEFAULT 'private',
                          price_cash DECIMAL(10,2),
                          favorite_count INT NOT NULL DEFAULT 0,
                          view_count INT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                          CONSTRAINT artworks_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                          CONSTRAINT artworks_title_len_check CHECK (LENGTH(title) BETWEEN 1 AND 255),
                          CONSTRAINT artworks_description_len_check CHECK (LENGTH(description) <= 1000),
                          CONSTRAINT artworks_price_cash_check CHECK (price_cash >= 0 AND price_cash <= 100),
                          CONSTRAINT artworks_favorite_count_check CHECK (favorite_count >= 0),
                          CONSTRAINT artworks_view_count_check CHECK (view_count >= 0)
);

-- 4. media: 스크린샷/GIF/영상
CREATE TABLE media (
                       media_id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       artwork_id BIGINT,
                       media_type media_type NOT NULL,
                       file_url VARCHAR(2048) NOT NULL,
                       duration_seconds INT,
                       thumbnail_url VARCHAR(2048),
                       visibility visibility_type NOT NULL DEFAULT 'private',
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                       CONSTRAINT media_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                       CONSTRAINT media_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE,
                       CONSTRAINT media_duration_seconds_check CHECK (
                           (media_type = 'image' AND duration_seconds IS NULL) OR
                           (media_type = 'gif' AND duration_seconds <= 3) OR
                           (media_type = 'video' AND duration_seconds <= 10)
                           )
);

-- artworks 테이블에 media 외래 키 제약 추가 (순환 참조 방지)
ALTER TABLE artworks ADD CONSTRAINT artworks_thumbnail_media_id_fk
    FOREIGN KEY (thumbnail_media_id) REFERENCES media(media_id) ON DELETE SET NULL;

-- 5. tags: 태그 마스터
CREATE TABLE tags (
                      tag_id SERIAL PRIMARY KEY,
                      tag_name VARCHAR(50) NOT NULL,
                      usage_count INT NOT NULL DEFAULT 0,
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                      CONSTRAINT tags_tag_name_uk UNIQUE (tag_name),
                      CONSTRAINT tags_tag_name_len_check CHECK (LENGTH(tag_name) BETWEEN 1 AND 50),
                      CONSTRAINT tags_usage_count_check CHECK (usage_count >= 0)
);

-- 6. artwork_tags: 작품-태그 관계
CREATE TABLE artwork_tags (
                              artwork_id BIGINT,
                              tag_id INT,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                              PRIMARY KEY (artwork_id, tag_id),
                              CONSTRAINT artwork_tags_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE,
                              CONSTRAINT artwork_tags_tag_id_fk FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

-- 7. qr_codes: QR 코드 관리
CREATE TABLE qr_codes (
                          qr_id BIGSERIAL PRIMARY KEY,
                          artwork_id BIGINT NOT NULL,
                          qr_token VARCHAR(255) NOT NULL DEFAULT gen_random_uuid()::text,
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
                                 CONSTRAINT qr_scan_history_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
                                 CONSTRAINT qr_scan_history_qr_id_fk FOREIGN KEY (qr_id) REFERENCES qr_codes(qr_id) ON DELETE CASCADE
);

-- 9. location_exhibitions: 장소 기반 전시
CREATE TABLE location_exhibitions (
                                      exhibition_id BIGSERIAL PRIMARY KEY,
                                      artwork_id BIGINT NOT NULL,
                                      location_name VARCHAR(255) NOT NULL,
    -- 4326은 GPS에서 사용하는 표준 좌표계(WGS 84)를 의미합니다.
                                      location_point GEOGRAPHY(Point, 4326) NOT NULL,
                                      radius_meters INT NOT NULL,
                                      is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                      start_date TIMESTAMP WITH TIME ZONE,
                                      end_date TIMESTAMP WITH TIME ZONE,
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                      CONSTRAINT location_exhibitions_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE,
                                      CONSTRAINT location_exhibitions_location_name_len_check CHECK (LENGTH(location_name) BETWEEN 1 AND 255),
                                      CONSTRAINT location_exhibitions_radius_meters_check CHECK (radius_meters > 0 AND radius_meters <= 1000),
                                      CONSTRAINT location_exhibitions_date_check CHECK (end_date IS NULL OR start_date < end_date)
);

-- 10. ai_generated_assets: AI 생성 에셋 관리
CREATE TABLE ai_generated_assets (
                                     asset_id BIGSERIAL PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     asset_type VARCHAR(20) NOT NULL,
                                     asset_data JSONB NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                     CONSTRAINT ai_generated_assets_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                     CONSTRAINT ai_generated_assets_asset_type_check CHECK (asset_type IN ('brush', 'palette'))
);

-- 11. ai_request_logs: AI 요청 로깅
CREATE TABLE ai_request_logs (
                                 log_id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 request_type ai_request_type NOT NULL,
                                 request_text TEXT,
                                 response_data JSONB,
                                 is_success BOOLEAN NOT NULL DEFAULT FALSE,
                                 error_code VARCHAR(50),
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
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
                               CONSTRAINT notifications_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                               CONSTRAINT notifications_title_len_check CHECK (LENGTH(title) BETWEEN 1 AND 255),
                               CONSTRAINT notifications_message_len_check CHECK (LENGTH(message) <= 500)
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
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                               CONSTRAINT user_profiles_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                               CONSTRAINT user_profiles_bio_len_check CHECK (LENGTH(bio) <= 100),
                               CONSTRAINT user_profiles_follower_count_check CHECK (follower_count >= 0),
                               CONSTRAINT user_profiles_following_count_check CHECK (following_count >= 0)
);

-- 14. favorite_artworks: 작품 즐겨찾기
CREATE TABLE favorite_artworks (
                                   user_id BIGINT,
                                   artwork_id BIGINT,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                   PRIMARY KEY (user_id, artwork_id),
                                   CONSTRAINT favorite_artworks_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                   CONSTRAINT favorite_artworks_artwork_id_fk FOREIGN KEY (artwork_id) REFERENCES artworks(artwork_id) ON DELETE CASCADE
);

-- 15. follows: 아티스트 팔로우
CREATE TABLE follows (
                         follower_id BIGINT,
                         following_id BIGINT,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                         PRIMARY KEY (follower_id, following_id),
                         CONSTRAINT follows_follower_id_fk FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
                         CONSTRAINT follows_following_id_fk FOREIGN KEY (following_id) REFERENCES users(user_id) ON DELETE CASCADE,
                         CONSTRAINT follows_self_follow_check CHECK (follower_id != following_id)
);

-- 16. media_tags: 미디어-태그 관계
CREATE TABLE media_tags (
                            media_id BIGINT,
                            tag_id INT,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
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
                          CONSTRAINT comments_content_len_check CHECK (LENGTH(content) BETWEEN 1 AND 200)
);

-- 19. user_preferences: 사용자 추천 설정
CREATE TABLE user_preferences (
                                  user_id BIGINT PRIMARY KEY,
                                  preferred_tags INT[] DEFAULT '{}',
                                  preferred_artists BIGINT[] DEFAULT '{}',
                                  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                  CONSTRAINT user_preferences_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ========== 제약 조건 및 트리거 생성 ==========

-- 작품당 태그 최대 5개 제한
CREATE OR REPLACE FUNCTION check_artwork_tag_limit() RETURNS TRIGGER AS $$
DECLARE
    tag_count INT;
BEGIN
SELECT COUNT(*) INTO tag_count
FROM artwork_tags WHERE artwork_id = NEW.artwork_id;

IF tag_count >= 5 THEN
        RAISE EXCEPTION 'A maximum of 5 tags can be selected per artwork.';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_artwork_tag_limit
    BEFORE INSERT ON artwork_tags
    FOR EACH ROW EXECUTE FUNCTION check_artwork_tag_limit();

-- QR 코드는 공개 작품만 생성 가능
CREATE OR REPLACE FUNCTION check_qr_artwork_visibility() RETURNS TRIGGER AS $$
DECLARE
    artwork_visibility visibility_type;
BEGIN
SELECT visibility INTO artwork_visibility
FROM artworks WHERE artwork_id = NEW.artwork_id;

IF artwork_visibility != 'public' THEN
        RAISE EXCEPTION 'QR codes can only be generated for public artworks.';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_qr_artwork_visibility
    BEFORE INSERT ON qr_codes
    FOR EACH ROW EXECUTE FUNCTION check_qr_artwork_visibility();

-- updated_at 자동 업데이트 트리거들
CREATE TRIGGER trigger_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_user_settings_updated_at BEFORE UPDATE ON user_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_artworks_updated_at BEFORE UPDATE ON artworks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_qr_codes_updated_at BEFORE UPDATE ON qr_codes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_location_exhibitions_updated_at BEFORE UPDATE ON location_exhibitions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_user_profiles_updated_at BEFORE UPDATE ON user_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_comments_updated_at BEFORE UPDATE ON comments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_user_preferences_updated_at BEFORE UPDATE ON user_preferences FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ========== 인덱스 생성 (명시적 이름 사용) ==========

CREATE INDEX users_provider_user_id_idx ON users(provider, provider_user_id);
CREATE INDEX users_email_idx ON users(email);
CREATE INDEX artworks_user_id_idx ON artworks(user_id);
CREATE INDEX artworks_visibility_idx ON artworks(visibility);
CREATE INDEX media_artwork_id_idx ON media(artwork_id);
CREATE INDEX artwork_tags_artwork_id_idx ON artwork_tags(artwork_id);
CREATE INDEX artwork_tags_tag_id_idx ON artwork_tags(tag_id);
CREATE INDEX qr_codes_qr_token_idx ON qr_codes(qr_token);
CREATE INDEX ai_request_logs_user_id_idx ON ai_request_logs(user_id);
CREATE INDEX ai_request_logs_request_type_idx ON ai_request_logs(request_type);
CREATE INDEX favorite_artworks_user_id_idx ON favorite_artworks(user_id);
CREATE INDEX favorite_artworks_artwork_id_idx ON favorite_artworks(artwork_id);
CREATE INDEX follows_follower_id_idx ON follows(follower_id);
CREATE INDEX follows_following_id_idx ON follows(following_id);
CREATE INDEX likes_user_id_idx ON likes(user_id);
CREATE INDEX likes_artwork_id_idx ON likes(artwork_id);
CREATE INDEX comments_artwork_id_idx ON comments(artwork_id);
CREATE INDEX comments_user_id_idx ON comments(user_id);
CREATE INDEX notifications_user_id_idx ON notifications(user_id);
CREATE INDEX location_exhibitions_location_point_gist ON location_exhibitions USING GIST (location_point);

-- ========== 기본 데이터 삽입 ==========
-- (참고) 이 부분은 향후 R__Seed_data.sql 같은 반복 가능한 마이그레이션 파일로 분리하는 것이 좋습니다.

-- 기본 태그 30개 삽입
INSERT INTO tags (tag_name) VALUES
                                ('추상화'), ('풍경화'), ('인물화'), ('정물화'), ('동물'),
                                ('건축물'), ('자연'), ('도시'), ('바다'), ('산'),
                                ('꽃'), ('나무'), ('구름'), ('일몰'), ('밤하늘'),
                                ('미니멀'), ('컬러풀'), ('모노크롬'), ('비현실적'), ('환상적'),
                                ('따뜻한'), ('차가운'), ('밝은'), ('어두운'), ('신비로운'),
                                ('평화로운'), ('역동적'), ('고요한'), ('화려한'), ('단순한')
ON CONFLICT (tag_name) DO NOTHING; -- 이미 존재하면 무시

-- 테스트 사용자 데이터
INSERT INTO users (nickname, email, provider, provider_user_id, role, current_mode)
VALUES ('test_artist', 'test@bauhaus.com', 'meta', 'test12345', 'artist', 'artist')
ON CONFLICT (provider, provider_user_id) DO NOTHING; -- 이미 존재하면 무시

-- 위에서 생성된 test_artist의 user_id를 가져와서 다른 테이블에 삽입
DO $$
    DECLARE
        test_user_id BIGINT;
BEGIN
SELECT user_id INTO test_user_id FROM users WHERE provider_user_id = 'test12345';

IF test_user_id IS NOT NULL THEN
INSERT INTO user_settings (user_id, stt_consent, ai_consent, data_training_consent)
VALUES (test_user_id, true, true, true)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_profiles (user_id, bio, bio_public, join_date_public)
VALUES (test_user_id, 'VR 아티스트입니다!', true, true)
ON CONFLICT (user_id) DO NOTHING;
END IF;
END
$$;