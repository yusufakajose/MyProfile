-- Webhook configuration table
CREATE TABLE IF NOT EXISTS webhook_configs (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  url VARCHAR(500) NOT NULL,
  secret VARCHAR(128) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_cfg_user ON webhook_configs(user_id);

-- Webhook delivery logs table
CREATE TABLE IF NOT EXISTS webhook_deliveries (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_type VARCHAR(100) NOT NULL,
  target_url VARCHAR(500) NOT NULL,
  attempt INTEGER NOT NULL,
  status_code INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  error_message VARCHAR(500),
  payload TEXT,
  next_attempt_at TIMESTAMP NULL,
  dead_lettered BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_webhook_user_created ON webhook_deliveries(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_webhook_retry_due ON webhook_deliveries(next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_webhook_user_dlq ON webhook_deliveries(user_id, dead_lettered);


