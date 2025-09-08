-- Add email verification fields to users
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP NULL;

-- Add session metadata to refresh tokens
ALTER TABLE refresh_tokens
  ADD COLUMN IF NOT EXISTS ip VARCHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS user_agent VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP NULL;

-- Email verification tokens table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_ver_tokens_user ON email_verification_tokens(user_id);

-- Password reset tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pwd_reset_tokens_user ON password_reset_tokens(user_id);


