import type { AuthMember } from '@/features/auth/model/authTypes.ts'
import type { CardStatus } from '@/features/card/model/cardTypes.ts'

export type HandoverStatus = 'WRITING' | 'PUBLISHED'

export type HandoverCardSummary = {
  id: string
  title: string
  status: CardStatus
  action: string | null
  directEvidenceCount: number
  recordedEvidenceCount: number
  updatedAt: string
}

export type HandoverProgress = { approvedCount: number; activeCount: number; ratio: number }

export type HandoverPackage = {
  id: string
  name: string
  description: string | null
  status: HandoverStatus
  predecessor: AuthMember
  successor: AuthMember | null
  progress: HandoverProgress
  cards: HandoverCardSummary[]
  createdAt: string
  publishedAt: string | null
}

export type CreatePackageRequest = {
  predecessorId: string
  successorId?: string
  name: string
  description?: string
}
