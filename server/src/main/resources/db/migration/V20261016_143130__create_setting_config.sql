CREATE TABLE setting_config (
  id           UUID        NOT NULL PRIMARY KEY,
  config_key   TEXT        NOT NULL UNIQUE,
  config_value TEXT        NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL,
  updated_at   TIMESTAMPTZ NOT NULL
);
