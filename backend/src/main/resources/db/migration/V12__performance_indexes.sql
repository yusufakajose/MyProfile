-- Enable trigram extension for faster ILIKE/substring searches
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Links: speed up user-scoped listings and active filtering with ordering
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'idx_links_user_active_display' AND n.nspname = 'public'
    ) THEN
        CREATE INDEX idx_links_user_active_display ON links(user_id, is_active, display_order);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'idx_links_user_display' AND n.nspname = 'public'
    ) THEN
        CREATE INDEX idx_links_user_display ON links(user_id, display_order);
    END IF;
END$$;

-- Trigram indexes for case-insensitive contains searches on title/url/description
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'idx_links_title_trgm' AND n.nspname = 'public'
    ) THEN
        CREATE INDEX idx_links_title_trgm ON links USING gin (lower(title) gin_trgm_ops);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'idx_links_url_trgm' AND n.nspname = 'public'
    ) THEN
        CREATE INDEX idx_links_url_trgm ON links USING gin (lower(url) gin_trgm_ops);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'idx_links_description_trgm' AND n.nspname = 'public'
    ) THEN
        CREATE INDEX idx_links_description_trgm ON links USING gin (lower(description) gin_trgm_ops);
    END IF;
END$$;

-- Functional index for LOWER(tag.name) used in queries
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'idx_tags_name_lower' AND n.nspname = 'public'
    ) THEN
        CREATE INDEX idx_tags_name_lower ON tags (lower(name));
    END IF;
END$$;


