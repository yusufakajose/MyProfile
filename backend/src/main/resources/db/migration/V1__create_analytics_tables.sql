CREATE TABLE IF NOT EXISTS link_click_daily_aggregate (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    day DATE NOT NULL,
    clicks BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_link_day ON link_click_daily_aggregate (username, link_id, day);
CREATE INDEX IF NOT EXISTS idx_aggregate_user_day ON link_click_daily_aggregate (username, day);
