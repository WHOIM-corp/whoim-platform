import type { DocumentType } from '@/features/evidence/model/evidenceTypes.ts'

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  RUNBOOK: '런북',
  INCIDENT: '장애 보고',
  JIRA: 'Jira 이슈',
  GIT: 'Git PR',
  WIKI: '위키 문서',
  MEETING_NOTE: '회의록',
  OTHER: '기타',
}

export const DOCUMENT_TYPE_OPTIONS: { value: DocumentType; label: string }[] = (
  Object.keys(DOCUMENT_TYPE_LABELS) as DocumentType[]
).map((value) => ({ value, label: DOCUMENT_TYPE_LABELS[value] }))
