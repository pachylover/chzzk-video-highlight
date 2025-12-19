-- Flyway migration: create chats hypertable and highlights table

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE TABLE IF NOT EXISTS chats (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  video_id text NOT NULL,
  ts timestamptz NOT NULL,
  user_id text,
  username text,
  message text,
  raw jsonb,
  created_at timestamptz DEFAULT now()
);

SELECT create_hypertable('chats', 'ts', if_not_exists => TRUE);
CREATE INDEX IF NOT EXISTS idx_chats_video_ts ON chats (video_id, ts DESC);

CREATE TABLE IF NOT EXISTS highlights (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  video_id text NOT NULL,
  minute timestamptz NOT NULL,
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
