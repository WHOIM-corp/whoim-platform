import type { EvidenceLinkType } from '@/features/card/model/cardTypes.ts'

export type ConsultEvidence = {
  type: EvidenceLinkType
  fileName: string | null
  excerpt: string
}

export type ConsultCard = {
  id: string
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
  evidences: ConsultEvidence[]
  verification: { predecessorConfirmedAt: string | null; approvedAt: string | null }
  explanation: string
}

export type ConsultQuestions = { questions: string[]; message: string | null }
export type ConsultSuggestions = { cards: ConsultCard[]; message: string | null }
export type ConsultAnswer = { question: string; answer: string }
