import type { ConsultCard } from '@/features/consult/model/consultTypes.ts'

export type ConsultEvidenceCounts = { direct: number; recorded: number }

export function evidenceCountsOf(card: ConsultCard): ConsultEvidenceCounts {
  const direct = card.evidences.filter((evidence) => evidence.type === 'DIRECT').length
  return { direct, recorded: card.evidences.length - direct }
}
