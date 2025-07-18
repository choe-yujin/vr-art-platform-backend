-- ========================================
-- V2: Google OAuth 지원을 위한 User 테이블 수정
-- ========================================

-- Users 테이블에 Google ID 컬럼 추가
ALTER TABLE users ADD COLUMN google_id VARCHAR(255);

-- Google ID에 UNIQUE 제약조건 추가
ALTER TABLE users ADD CONSTRAINT uk_users_google_id UNIQUE (google_id);

-- 기존 provider/provider_user_id 방식과 호환성을 위한 인덱스
CREATE INDEX idx_users_provider_user_id ON users(provider, provider_user_id);

-- Email 컬럼에 UNIQUE 제약조건 추가 (이미 있다면 무시)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'uk_users_email' 
        AND table_name = 'users'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
    END IF;
END $$;

-- Username 컬럼 추가 (nickname을 username으로 대체)
ALTER TABLE users ADD COLUMN username VARCHAR(50);

-- 기존 nickname 데이터를 username으로 복사
UPDATE users SET username = nickname WHERE username IS NULL;

-- Username을 NOT NULL로 변경
ALTER TABLE users ALTER COLUMN username SET NOT NULL;

-- Profile image URL 컬럼 추가
ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(500);

-- 활성화 상태 컬럼 추가
ALTER TABLE users ADD COLUMN is_active BOOLEAN DEFAULT TRUE NOT NULL;

-- 마지막 로그인 시간 컬럼 추가
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;

-- 기존 데이터의 provider/provider_user_id를 google_id로 마이그레이션
UPDATE users 
SET google_id = provider_user_id 
WHERE provider = 'google' AND google_id IS NULL;

-- 코멘트 추가
COMMENT ON COLUMN users.google_id IS 'Google OAuth Subject ID';
COMMENT ON COLUMN users.username IS 'Display name for the user';
COMMENT ON COLUMN users.profile_image_url IS 'URL to user profile image from Google';
COMMENT ON COLUMN users.is_active IS 'Whether the user account is active';
COMMENT ON COLUMN users.last_login_at IS 'Last login timestamp';
