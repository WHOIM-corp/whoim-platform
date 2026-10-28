import type { CardStatus } from '@/features/card/model/cardTypes.ts'
import type { HandoverPackage } from '@/features/handover/model/handoverTypes.ts'

export function countCardsWithStatus(handoverPackage: HandoverPackage, status: CardStatus): number {
  return handoverPackage.cards.filter((card) => card.status === status).length
}
