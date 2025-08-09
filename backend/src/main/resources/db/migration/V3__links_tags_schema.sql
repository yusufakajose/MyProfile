-- Ensure links table has needed columns (if not using JPA update)
ALTER TABLE IF EXISTS links
  ADD COLUMN IF NOT EXISTS alias VARCHAR(100) UNIQUE,
  ADD COLUMN IF NOT EXISTS start_at TIMESTAMP NULL,
  ADD COLUMN IF NOT EXISTS end_at TIMESTAMP NULL,
  ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS click_count BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Basic FK if not present
DO $$ BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_links_user' AND table_name = 'links'
  ) THEN
    ALTER TABLE links
      ADD CONSTRAINT fk_links_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
END $$;

-- Tags table
CREATE TABLE IF NOT EXISTS tags (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(64) NOT NULL UNIQUE
);

-- Link-Tags join table
CREATE TABLE IF NOT EXISTS link_tags (
  link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
  tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (link_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_link_tags_link ON link_tags(link_id);
CREATE INDEX IF NOT EXISTS idx_link_tags_tag ON link_tags(tag_id);


