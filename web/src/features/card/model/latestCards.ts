import type { CardSummary } from '@/features/card/model/cardTypes.ts'

const LATEST_CARD_LIMIT = 4

// 후임자 홈에 보여줄 가장 최근에 갱신된 Card다.
export function latestCards(cards: CardSummary[]): CardSummary[] {
  return [...cards].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)).slice(0, LATEST_CARD_LIMIT)
}
