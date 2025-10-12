-- Add profile view count to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_view_count BIGINT NOT NULL DEFAULT 0;

