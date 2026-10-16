CREATE TABLE gap (
  id                  UUID        NOT NULL PRIMARY KEY,
  handover_package_id UUID        NOT NULL REFERENCES handover_package(id),
  type                TEXT        NOT NULL,
  summary             TEXT        NOT NULL,
  question            TEXT        NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL,
  updated_at          TIMESTAMPTZ NOT NULL,
  CONSTRAINT gap_type_check CHECK (type IN ('TRIGGER_GAP', 'ROOT_CAUSE_GAP', 'ASSESSMENT_GAP', 'CHOICE_GAP', 'EXCEPTION_EXPOSURE_GAP', 'EVIDENCE_CONFLICT', 'UNDOCUMENTED_PATTERN'))
);

CREATE INDEX idx_gap_handover_package_id ON gap (handover_package_id);

CREATE TABLE gap_anchor (
  id           UUID        NOT NULL PRIMARY KEY,
  gap_id       UUID        NOT NULL REFERENCES gap(id),
  document_id  UUID        NOT NULL REFERENCES evidence_document(id),
  start_offset INT         NOT NULL,
  end_offset   INT         NOT NULL,
  excerpt      TEXT        NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL,
  updated_at   TIMESTAMPTZ NOT NULL,
  CONSTRAINT gap_anchor_offset_check CHECK (start_offset >= 0 AND end_offset > start_offset)
);

CREATE INDEX idx_gap_anchor_gap_id ON gap_anchor (gap_id);
