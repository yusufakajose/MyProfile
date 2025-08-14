-- Core domain schema required before analytics migrations

-- Users table
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  display_name VARCHAR(100),
  bio VARCHAR(500),
  profile_image_url VARCHAR(200),
  theme_primary_color VARCHAR(20),
  theme_accent_color VARCHAR(20),
  theme_background_color VARCHAR(20),
  theme_text_color VARCHAR(20)
);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE
);

-- User-Roles join table
CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Links table
CREATE TABLE IF NOT EXISTS links (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  url VARCHAR(500) NOT NULL,
  description VARCHAR(200),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  display_order INTEGER NOT NULL DEFAULT 0,
  click_count BIGINT NOT NULL DEFAULT 0,
  alias VARCHAR(100) UNIQUE,
  start_at TIMESTAMP NULL,
  end_at TIMESTAMP NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_links_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Helpful index
CREATE INDEX IF NOT EXISTS idx_links_user_id ON links(user_id);


