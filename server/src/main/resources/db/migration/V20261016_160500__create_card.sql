CREATE TABLE trace_card (
  id                  UUID        NOT NULL PRIMARY KEY,
  handover_package_id UUID        NOT NULL REFERENCES handover_package(id),
  interview_id        UUID        NOT NULL REFERENCES interview(id),
  title               TEXT        NOT NULL,
  status              TEXT        NOT NULL,
  situation           TEXT,
  signal              TEXT,
  cause               TEXT,
  condition           TEXT,
  judgment            TEXT,
  alternative         TEXT,
  action              TEXT,
  reason              TEXT,
  exception           TEXT,
  risk                TEXT,
  created_at          TIMESTAMPTZ NOT NULL,
  updated_at          TIMESTAMPTZ NOT NULL,
  CONSTRAINT trace_card_interview_key UNIQUE (interview_id),
  CONSTRAINT trace_card_status_check CHECK (status IN ('DRAFT', 'PREDECESSOR_CONFIRMED', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_trace_card_handover_package_id_status ON trace_card (handover_package_id, status);

CREATE TABLE card_evidence_link (
  id           UUID        NOT NULL PRIMARY KEY,
  card_id      UUID        NOT NULL REFERENCES trace_card(id),
  type         TEXT        NOT NULL,
  turn_id      UUID        REFERENCES interview_turn(id),
  document_id  UUID        REFERENCES evidence_document(id),
  start_offset INT,
  end_offset   INT,
  excerpt      TEXT        NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL,
  updated_at   TIMESTAMPTZ NOT NULL,
  CONSTRAINT card_evidence_link_type_check CHECK (type IN ('DIRECT', 'RECORDED')),
  CONSTRAINT card_evidence_link_shape_check CHECK ((type = 'DIRECT' AND turn_id IS NOT NULL AND document_id IS NULL AND start_offset IS NULL AND end_offset IS NULL) OR (type = 'RECORDED' AND turn_id IS NULL AND document_id IS NOT NULL AND start_offset IS NOT NULL AND end_offset IS NOT NULL AND start_offset >= 0 AND end_offset > start_offset))
);

CREATE INDEX idx_card_evidence_link_card_id ON card_evidence_link (card_id);

CREATE TABLE card_state_history (
  id          UUID        NOT NULL PRIMARY KEY,
  card_id     UUID        NOT NULL REFERENCES trace_card(id),
  from_status TEXT,
  to_status   TEXT        NOT NULL,
  actor_id    UUID        REFERENCES member(id),
  reason      TEXT,
  created_at  TIMESTAMPTZ NOT NULL,
  updated_at  TIMESTAMPTZ NOT NULL,
  CONSTRAINT card_state_history_from_status_check CHECK (from_status IS NULL OR from_status IN ('DRAFT', 'PREDECESSOR_CONFIRMED', 'APPROVED', 'REJECTED')),
  CONSTRAINT card_state_history_to_status_check CHECK (to_status IN ('DRAFT', 'PREDECESSOR_CONFIRMED', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_card_state_history_card_id ON card_state_history (card_id);
