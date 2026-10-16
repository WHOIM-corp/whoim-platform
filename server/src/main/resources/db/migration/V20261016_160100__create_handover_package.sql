CREATE TABLE handover_package (
  id              UUID        NOT NULL PRIMARY KEY,
  predecessor_id  UUID        NOT NULL REFERENCES member(id),
  successor_id    UUID        REFERENCES member(id),
  name            TEXT        NOT NULL,
  description     TEXT,
  status          TEXT        NOT NULL,
  published_at    TIMESTAMPTZ,
  published_by_id UUID        REFERENCES member(id),
  created_at      TIMESTAMPTZ NOT NULL,
  updated_at      TIMESTAMPTZ NOT NULL,
  CONSTRAINT handover_package_status_check CHECK (status IN ('WRITING', 'PUBLISHED')),
  CONSTRAINT handover_package_published_check CHECK ((status = 'WRITING' AND published_at IS NULL AND published_by_id IS NULL) OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND published_by_id IS NOT NULL)),
  CONSTRAINT handover_package_predecessor_name_key UNIQUE (predecessor_id, name)
);

CREATE INDEX idx_handover_package_predecessor_id ON handover_package (predecessor_id);
CREATE INDEX idx_handover_package_successor_id ON handover_package (successor_id);
