export type GapType =
  | 'TRIGGER_GAP'
  | 'ROOT_CAUSE_GAP'
  | 'ASSESSMENT_GAP'
  | 'CHOICE_GAP'
  | 'EXCEPTION_EXPOSURE_GAP'
  | 'EVIDENCE_CONFLICT'
  | 'UNDOCUMENTED_PATTERN'

export type GapProgress = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'PARTIAL' | 'REJECTED'

export type GapAnchor = {
  documentId: string
  fileName: string
  startOffset: number
  endOffset: number
  excerpt: string
}

export type Gap = {
  id: string
  type: GapType
  summary: string
  question: string
  anchors: GapAnchor[]
  progress: GapProgress
  interviewId: string | null
  cardId: string | null
}
