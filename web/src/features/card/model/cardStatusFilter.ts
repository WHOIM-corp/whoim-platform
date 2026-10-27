import type { CardStatus } from '@/features/card/model/cardTypes.ts'

export type CardStatusFilter = CardStatus | 'ALL'

const CARD_STATUSES: readonly CardStatus[] = ['DRAFT', 'PREDECESSOR_CONFIRMED', 'APPROVED', 'REJECTED']

// URL 쿼리 값을 믿지 않고, 아는 상태가 아니면 전체로 본다.
export function parseCardStatusFilter(value: string | null): CardStatusFilter {
  return CARD_STATUSES.find((status) => status === value) ?? 'ALL'
}

export function filterByStatus<T extends { status: CardStatus }>(cards: T[], filter: CardStatusFilter): T[] {
  return filter === 'ALL' ? cards : cards.filter((card) => card.status === filter)
}
