import { CARD_STATUS_META } from '@/features/card/model/cardStatusMeta.ts'
import type { CardStatus } from '@/features/card/model/cardTypes.ts'
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge.tsx'

type CardStatusBadgeProps = {
  status: CardStatus
}

export function CardStatusBadge({ status }: CardStatusBadgeProps) {
  const { label, tone, icon } = CARD_STATUS_META[status]
  return <StatusBadge tone={tone} icon={icon} label={label} />
}
