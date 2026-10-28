import type { Position } from '@/features/auth/model/authTypes.ts'
import type { CardStatus } from '@/features/card/model/cardTypes.ts'

// 승인자에게 초안은 서버가 상세를 404로 숨기므로 열지 않고 이 문구를 보여준다.
export function closedReasonFor(position: Position | null, status: CardStatus): string | undefined {
  return position === 'REVIEWER' && status === 'DRAFT' ? '전임자 확인 전' : undefined
}
