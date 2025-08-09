-- Add unique visitors to main daily aggregate
ALTER TABLE IF EXISTS link_click_daily_aggregate
    ADD COLUMN IF NOT EXISTS unique_visitors BIGINT NOT NULL DEFAULT 0;

-- Referrer daily aggregates (domain-level)
CREATE TABLE IF NOT EXISTS link_referrer_daily_aggregate (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    day DATE NOT NULL,
    referrer_domain VARCHAR(255) NOT NULL,
    clicks BIGINT NOT NULL DEFAULT 0,
    unique_visitors BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ref_user_link_day_domain
  ON link_referrer_daily_aggregate (username, link_id, day, referrer_domain);
CREATE INDEX IF NOT EXISTS idx_ref_user_day
  ON link_referrer_daily_aggregate (username, day);

-- Device daily aggregates (simple type classifier)
CREATE TABLE IF NOT EXISTS link_device_daily_aggregate (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    day DATE NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    clicks BIGINT NOT NULL DEFAULT 0,
    unique_visitors BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_dev_user_link_day_type
  ON link_device_daily_aggregate (username, link_id, day, device_type);
CREATE INDEX IF NOT EXISTS idx_dev_user_day
  ON link_device_daily_aggregate (username, day);


