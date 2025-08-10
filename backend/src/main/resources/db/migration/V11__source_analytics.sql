CREATE TABLE IF NOT EXISTS link_source_daily_aggregate (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    day DATE NOT NULL,
    source VARCHAR(64) NOT NULL,
    clicks BIGINT NOT NULL DEFAULT 0,
    unique_visitors BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_link_day_src UNIQUE (username, link_id, day, source)
);

CREATE INDEX IF NOT EXISTS idx_source_user_day ON link_source_daily_aggregate (username, day);


