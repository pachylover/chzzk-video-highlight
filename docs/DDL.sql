-- DDL: CHZZK 하이라이트 서비스 (참고용)
-- 이 파일은 프로젝트에서 사용하는 기본 스키마와 유틸리티 쿼리를 문서화합니다.
-- 실제 배포 시에는 Flyway 마이그레이션(V1/V2 등)을 사용하세요.

-- 확장: pgcrypto (UUID), timescaledb
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- 채팅 테이블 (Timescale hypertable)
CREATE TABLE IF NOT EXISTS chats (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  video_id text NOT NULL,
  ts timestamptz NOT NULL,                     -- 수집 시점(UTC)
  message_time bigint,                         -- API에서 제공하는 messageTime (ms)
  player_message_time bigint,                  -- API의 playerMessageTime (player 기준 밀리초)
  user_id text,                                -- userIdHash
  username text,                               -- profile에서 nickname 추출
  message text,                                -- 채팅 콘텐츠
  raw jsonb,                                   -- 원본 이벤트 전체
  created_at timestamptz DEFAULT now()
);

-- Hypertable로 변환 (timescaledb)
SELECT create_hypertable('chats', 'ts', if_not_exists => TRUE);

-- 인덱스: video 조회 및 시간 내림차순
CREATE INDEX IF NOT EXISTS idx_chats_video_ts ON chats (video_id, ts DESC);

-- 중복 방지용 유니크 제약(동일 비디오의 동일 message_time + user_id)
CREATE UNIQUE INDEX IF NOT EXISTS uniq_chats_video_message_user ON chats (video_id, message_time, user_id);

-- 하이라이트 메타 정보
CREATE TABLE IF NOT EXISTS highlights (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  video_id text NOT NULL,
  minute timestamptz NOT NULL,     -- time_bucket('1 minute', ts) 단위로 저장
  start_ts timestamptz,
  end_ts timestamptz,
  chat_count integer,
  title text,
  summary text,
  chat_snapshot jsonb,
  status text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_highlights_video_minute ON highlights (video_id, minute);

-- 편의 함수: 특정 비디오의 최다 채팅 1분(분 단위와 카운트) 반환
CREATE OR REPLACE FUNCTION get_peak_minute(p_video_id text)
RETURNS TABLE(minute timestamptz, cnt bigint) LANGUAGE sql AS $$
  SELECT time_bucket('1 minute', ts) AS minute, count(*) AS cnt
  FROM chats
  WHERE video_id = $1
  GROUP BY minute
  ORDER BY cnt DESC
  LIMIT 1;
$$;

-- 샘플 쿼리: 특정 비디오의 하이라이트 후보(peak minute, 앞뒤 30s/90s 확장)
-- SELECT * FROM get_peak_minute('10442147');
-- SELECT get_peak_minute('10442147');

-- 권장: 대량 삽입 시 COPY 또는 INSERT ... ON CONFLICT DO NOTHING(유니크 인덱스 활용)로 중복 방지

-- 문서 끝
