-- Flyway baseline (V1): create chats and highlights tables for plain PostgreSQL

-- pgcrypto for UUID helpers (kept for compatibility where used)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- chats: final shape used by JPA entity (bigserial id, player_message_time epoch ms)
CREATE TABLE IF NOT EXISTS chats (
  id bigserial PRIMARY KEY,
  video_id text NOT NULL,
  user_id text,
  username text,
  message text,
  player_message_time bigint,
  created_at timestamptz DEFAULT now()
);

-- dedupe & lookup indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_uniq_chats_dedup ON chats (video_id, user_id, player_message_time);
CREATE INDEX IF NOT EXISTS idx_chats_video_ts ON chats (video_id DESC);

-- highlights: final shape (epoch millis stored as bigint)
CREATE TABLE IF NOT EXISTS highlights (
  id bigserial PRIMARY KEY,
  video_id text NOT NULL,
  minute bigint NOT NULL,         -- epoch millis
  start_ts bigint,
  end_ts bigint,
  chat_count integer,
  title text,
  summary text,
  chat_snapshot jsonb DEFAULT '{}'::jsonb,
  status text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_highlights_video_minute ON highlights (video_id, minute);
