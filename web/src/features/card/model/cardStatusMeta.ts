import { Check, Clock, PencilLine, X, type LucideIcon } from 'lucide-react'
import type { CardStatus } from '@/features/card/model/cardTypes.ts'
import type { StatusBadgeTone } from '@/shared/components/StatusBadge/StatusBadge.tsx'

export const CARD_STATUS_META: Record<CardStatus, { label: string; tone: StatusBadgeTone; icon: LucideIcon }> = {
  DRAFT: { label: '초안', tone: 'neutral', icon: PencilLine },
  PREDECESSOR_CONFIRMED: { label: '검수 대기', tone: 'pending', icon: Clock },
  APPROVED: { label: '승인됨', tone: 'approved', icon: Check },
  REJECTED: { label: '반려', tone: 'rejected', icon: X },
}
