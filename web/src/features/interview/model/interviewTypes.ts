import type { EvidenceLinkType, TraceElement } from '@/features/card/model/cardTypes.ts'

export type InterviewStatus = 'IN_PROGRESS' | 'COMPLETED'
export type InterviewEndReason = 'CHECKLIST_COMPLETE' | 'TURN_LIMIT'

export type InterviewTurn = {
  turnNumber: number
  question: string
  answer: string | null
  processed: boolean
}

export type TraceStateEntry = {
  status: 'FILLED' | 'UNKNOWN'
  content: string | null
  source: EvidenceLinkType | null
  turnNumber: number
}

export type Interview = {
  id: string
  gapId: string
  status: InterviewStatus
  endReason: InterviewEndReason | null
  currentQuestion: string | null
  turns: InterviewTurn[]
  traceState: Partial<Record<TraceElement, TraceStateEntry>>
}
