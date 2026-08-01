-- Flyway V3: 관리자 계정, 배너, 안내문구 테이블 및 채팅 검색 인덱스

-- 관리자 계정 (BCrypt 해시 저장)
CREATE TABLE IF NOT EXISTS admin_users (
  id bigserial PRIMARY KEY,
  username text NOT NULL UNIQUE,
  password_hash text NOT NULL,
  role text NOT NULL DEFAULT 'ADMIN',
  created_at timestamptz DEFAULT now()
);

-- 배너 (이미지 + 링크)
CREATE TABLE IF NOT EXISTS banners (
  id bigserial PRIMARY KEY,
  title text,
  image_url text NOT NULL,
  link_url text,
  sort_order integer NOT NULL DEFAULT 0,
  is_active boolean NOT NULL DEFAULT true,
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_banners_active_order ON banners (is_active, sort_order);

-- 안내 문구 (상단 바 텍스트)
CREATE TABLE IF NOT EXISTS announcements (
  id bigserial PRIMARY KEY,
  message text NOT NULL,
  level text NOT NULL DEFAULT 'INFO',   -- INFO | WARNING | SUCCESS
  link_url text,
  is_active boolean NOT NULL DEFAULT true,
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_announcements_active ON announcements (is_active);

-- 채팅 검색 인덱스 (pg_trgm 은 V2 에서 생성됨) — ILIKE 부분검색 가속
CREATE INDEX IF NOT EXISTS idx_chats_message_trgm ON chats USING gin (message gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_chats_username_trgm ON chats USING gin (username gin_trgm_ops);

-- 최근 하이라이트 조회 가속
CREATE INDEX IF NOT EXISTS idx_highlights_created_at ON highlights (created_at DESC);
