import type { Position } from '@/features/auth/model/authTypes.ts'
import type { CardStatus } from '@/features/card/model/cardTypes.ts'

export type CardQueue = { status: CardStatus; label: string }

// 직책마다 다음 처리를 기다리는 Card 상태다. 후임자는 큐가 없다.
export const CARD_QUEUE_BY_POSITION: Partial<Record<Position, CardQueue>> = {
  REVIEWER: { status: 'PREDECESSOR_CONFIRMED', label: '검수 대기' },
  PREDECESSOR: { status: 'DRAFT', label: '확인 대기' },
}
