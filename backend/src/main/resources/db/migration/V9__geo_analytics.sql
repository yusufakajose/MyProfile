-- Geo analytics: per-country daily aggregates
CREATE TABLE IF NOT EXISTS link_geo_daily_aggregate (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    day DATE NOT NULL,
    country CHAR(2) NOT NULL,
    clicks BIGINT NOT NULL DEFAULT 0,
    unique_visitors BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_link_day_country UNIQUE (username, link_id, day, country)
);

CREATE INDEX IF NOT EXISTS idx_geo_user_day ON link_geo_daily_aggregate(username, day);


