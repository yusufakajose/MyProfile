CREATE TABLE IF NOT EXISTS link_variant_daily_aggregate (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
  variant_id BIGINT NOT NULL REFERENCES link_variants(id) ON DELETE CASCADE,
  day DATE NOT NULL,
  clicks BIGINT NOT NULL DEFAULT 0,
  unique_visitors BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_link_variant_day UNIQUE (username, link_id, variant_id, day)
);

CREATE INDEX IF NOT EXISTS idx_lvda_user_day ON link_variant_daily_aggregate(username, day);
CREATE INDEX IF NOT EXISTS idx_lvda_link_variant ON link_variant_daily_aggregate(link_id, variant_id);


