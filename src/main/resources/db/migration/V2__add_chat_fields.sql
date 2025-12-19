-- Add additional columns to chats to store original API timestamp fields and add uniqueness constraint to help deduplication

ALTER TABLE chats ADD COLUMN IF NOT EXISTS message_time bigint;
ALTER TABLE chats ADD COLUMN IF NOT EXISTS player_message_time bigint;

-- Unique index to avoid inserting the same chat repeatedly: by video_id + message_time + user_id
CREATE UNIQUE INDEX IF NOT EXISTS uniq_chats_video_message_user ON chats (video_id, message_time, user_id);
