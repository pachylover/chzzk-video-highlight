-- Flyway V4: 하이라이트에 영상 제목 컬럼 추가 + 채팅 타임라인/재수집 관련 인덱스

-- 하이라이트 목록(홈/관리자)에서 videoId 대신 영상 제목을 노출하기 위한 컬럼.
-- 기존 행은 NULL 로 남고, 배포 후 VideoTitleBackfillRunner 가 치지직 API 로 채운다.
ALTER TABLE highlights
    ADD COLUMN IF NOT EXISTS video_title text;

-- 분당 채팅량 집계(GROUP BY player_message_time/60000) 및 채팅 존재 여부 확인 가속
CREATE INDEX IF NOT EXISTS idx_chats_video_player_time ON chats (video_id, player_message_time);
