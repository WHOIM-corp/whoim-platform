CREATE TABLE interview (
  id                  UUID        NOT NULL PRIMARY KEY,
  handover_package_id UUID        NOT NULL REFERENCES handover_package(id),
  gap_id              UUID        NOT NULL REFERENCES gap(id),
  status              TEXT        NOT NULL,
  end_reason          TEXT,
  trace_state         TEXT        NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL,
  updated_at          TIMESTAMPTZ NOT NULL,
  CONSTRAINT interview_gap_key UNIQUE (gap_id),
  CONSTRAINT interview_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
  CONSTRAINT interview_end_reason_check CHECK (end_reason IS NULL OR end_reason IN ('CHECKLIST_COMPLETE', 'TURN_LIMIT')),
  CONSTRAINT interview_completed_check CHECK ((status = 'COMPLETED') = (end_reason IS NOT NULL))
);

CREATE INDEX idx_interview_handover_package_id ON interview (handover_package_id);

CREATE TABLE interview_turn (
  id              UUID        NOT NULL PRIMARY KEY,
  interview_id    UUID        NOT NULL REFERENCES interview(id),
  turn_number     INT         NOT NULL,
  question        TEXT        NOT NULL,
  answer          TEXT,
  extracted_trace TEXT,
  created_at      TIMESTAMPTZ NOT NULL,
  updated_at      TIMESTAMPTZ NOT NULL,
  CONSTRAINT interview_turn_interview_turn_number_key UNIQUE (interview_id, turn_number),
  CONSTRAINT interview_turn_turn_number_check CHECK (turn_number >= 1)
);
