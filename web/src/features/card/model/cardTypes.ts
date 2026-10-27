export type CardStatus = 'DRAFT' | 'PREDECESSOR_CONFIRMED' | 'APPROVED' | 'REJECTED'

export type TraceElement =
  | 'SITUATION'
  | 'SIGNAL'
  | 'CAUSE'
  | 'CONDITION'
  | 'JUDGMENT'
  | 'ALTERNATIVE'
  | 'ACTION'
  | 'REASON'
  | 'EXCEPTION'
  | 'RISK'

export type EvidenceLinkType = 'DIRECT' | 'RECORDED'

export type CardSummary = {
  id: string
  handoverPackageId: string
  interviewId: string
  title: string
  status: CardStatus
  action: string | null
  directEvidenceCount: number
  recordedEvidenceCount: number
  updatedAt: string
}

export type CardEvidenceLink = {
  type: EvidenceLinkType
  turnNumber: number | null
  documentId: string | null
  fileName: string | null
  startOffset: number | null
  endOffset: number | null
  excerpt: string
  elements: TraceElement[]
}

export type CardStateHistory = {
  fromStatus: CardStatus | null
  toStatus: CardStatus
  actorUsername: string | null
  actorName: string | null
  reason: string | null
  createdAt: string
}

type CardTraceFields = {
  title: string
  situation: string
  signal: string
  cause: string
  condition: string
  judgment: string
  alternative: string
  action: string
  reason: string
  exception: string
  risk: string
}

export type CardDetail = CardTraceFields & {
  id: string
  handoverPackageId: string
  interviewId: string
  status: CardStatus
  evidenceLinks: CardEvidenceLink[]
  stateHistory: CardStateHistory[]
  createdAt: string
  updatedAt: string
}

// Card 생성 응답. 상세와 달리 상태 이력과 handoverPackageId가 없다.
export type CreatedCard = CardTraceFields & {
  id: string
  interviewId: string
  status: CardStatus
  evidenceLinks: CardEvidenceLink[]
  createdAt: string
}
