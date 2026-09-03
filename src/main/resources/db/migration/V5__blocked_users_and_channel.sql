-- Flyway V5: 비수집(블랙리스트) 회원 관리 + 하이라이트 채널 정보

-- 수집 거부 회원. uid 는 치지직 채널 URL 의 긴 문자열(= chats.user_id 의 userIdHash).
-- 여기에 등록된 uid 는 채팅 수집 단계에서 제외되고, 등록 시점에 기존 채팅도 삭제된다.
CREATE TABLE IF NOT EXISTS blocked_users (
  id bigserial PRIMARY KEY,
  uid text NOT NULL UNIQUE,
  nickname text,
  memo text,
  deleted_chats integer NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now()
);

-- 블랙리스트 등록 시 해당 회원의 채팅을 지우기 위한 인덱스
CREATE INDEX IF NOT EXISTS idx_chats_user_id ON chats (user_id);

-- 하이라이트에 스트리머(채널) 정보 추가.
-- 기존 행은 NULL 로 남고 VideoMetaBackfillRunner 가 치지직 API 로 채운다.
ALTER TABLE highlights
    ADD COLUMN IF NOT EXISTS channel_id text,
    ADD COLUMN IF NOT EXISTS channel_name text;

-- 같은 스트리머의 다른 하이라이트 조회 가속
CREATE INDEX IF NOT EXISTS idx_highlights_channel_created ON highlights (channel_id, created_at DESC);
