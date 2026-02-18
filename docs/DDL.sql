-- DDL: CHZZK 하이라이트 서비스 (참고용)
-- 이 파일은 프로젝트에서 사용하는 기본 스키마와 유틸리티 쿼리를 문서화합니다.
-- 실제 배포 시에는 Flyway 마이그레이션(V1/V2 등)을 사용하세요.

-- 확장: pgcrypto (UUID)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 채팅 테이블
CREATE TABLE IF NOT EXISTS chats (
  id bigserial PRIMARY KEY,
  video_id text NOT NULL,
  player_message_time bigint,                  -- API의 playerMessageTime (player 기준 밀리초)
  -- (기존의 timestamptz `ts` 컬럼은 스키마 변경으로 제거됨)
  user_id text,                                -- userIdHash
  username text,                               -- profile에서 nickname 추출
  message text,                                -- 채팅 콘텐츠
  created_at timestamptz DEFAULT now()
);

-- 인덱스: video 조회 및 시간 내림차순
CREATE INDEX IF NOT EXISTS idx_chats_video_ts ON chats (video_id DESC);

-- 중복 방지용 유니크 제약(동일 비디오의 동일 message_time + user_id)
CREATE UNIQUE INDEX IF NOT EXISTS uniq_chats_video_message_user ON chats (video_id, message_time, user_id);

-- 하이라이트 메타 정보
CREATE TABLE IF NOT EXISTS highlights (
  id bigserial PRIMARY KEY,
  video_id text NOT NULL,
  minute bigint NOT NULL,           -- epoch millis (bigint)
  start_ts bigint,
  end_ts bigint,
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
RETURNS TABLE(minute bigint, cnt bigint) LANGUAGE sql AS $$
  SELECT (player_message_time - (player_message_time % 60000)) AS minute, count(*) AS cnt
  FROM chats
  WHERE video_id = $1
  GROUP BY (player_message_time - (player_message_time % 60000))
  ORDER BY cnt DESC
  LIMIT 1;
$$;

-- 샘플 쿼리: 특정 비디오의 하이라이트 후보(peak minute, 앞뒤 30s/90s 확장)
-- SELECT * FROM get_peak_minute('10442147');
-- SELECT get_peak_minute('10442147');

-- 권장: 대량 삽입 시 COPY 또는 INSERT ... ON CONFLICT DO NOTHING(유니크 인덱스 활용)로 중복 방지

-- 문서 끝
