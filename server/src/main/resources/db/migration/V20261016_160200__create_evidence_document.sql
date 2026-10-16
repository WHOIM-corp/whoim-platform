CREATE TABLE evidence_document (
  id                  UUID        NOT NULL PRIMARY KEY,
  handover_package_id UUID        NOT NULL REFERENCES handover_package(id),
  file_name           TEXT        NOT NULL,
  document_type       TEXT        NOT NULL,
  content             TEXT        NOT NULL,
  uploaded_by_id      UUID        NOT NULL REFERENCES member(id),
  created_at          TIMESTAMPTZ NOT NULL,
  updated_at          TIMESTAMPTZ NOT NULL,
  CONSTRAINT evidence_document_document_type_check CHECK (document_type IN ('RUNBOOK', 'INCIDENT', 'JIRA', 'GIT', 'WIKI', 'MEETING_NOTE', 'OTHER'))
);

CREATE INDEX idx_evidence_document_handover_package_id ON evidence_document (handover_package_id);
