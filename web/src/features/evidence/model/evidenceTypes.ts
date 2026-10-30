export type DocumentType =
  | 'RUNBOOK'
  | 'INCIDENT'
  | 'JIRA'
  | 'GIT'
  | 'WIKI'
  | 'MEETING_NOTE'
  | 'OTHER'

export type EvidenceDocument = {
  id: string
  fileName: string
  documentType: DocumentType
  characterCount: number
  uploaderId: string
  uploaderName: string
  uploadedAt: string
}
