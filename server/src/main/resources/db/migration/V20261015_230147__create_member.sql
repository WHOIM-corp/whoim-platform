CREATE TABLE member (
  id         UUID        NOT NULL PRIMARY KEY,
  username   TEXT        NOT NULL UNIQUE,
  password   TEXT        NOT NULL,
  name       TEXT        NOT NULL,
  role       TEXT        NOT NULL,
  position   TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT member_role_check CHECK (role IN ('USER', 'ADMIN')),
  CONSTRAINT member_position_check CHECK (position IS NULL OR position IN ('PREDECESSOR', 'SUCCESSOR', 'REVIEWER')),
  CONSTRAINT member_role_position_check CHECK (
    (role = 'ADMIN' AND position IS NULL) OR (role = 'USER' AND position IS NOT NULL)
  )
);
