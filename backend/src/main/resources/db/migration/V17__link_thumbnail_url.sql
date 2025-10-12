-- Add thumbnail URL to links table for Open Graph preview images
ALTER TABLE links ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);

