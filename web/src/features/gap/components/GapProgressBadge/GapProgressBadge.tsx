import { gapProgressLabels } from '@/features/gap/model/gapLabels.ts'
import type { GapProgress } from '@/features/gap/model/gapTypes.ts'
import { StatusBadge, type StatusBadgeTone } from '@/shared/components/StatusBadge/StatusBadge.tsx'

const GAP_PROGRESS_TONES: Record<GapProgress, StatusBadgeTone> = {
  NOT_STARTED: 'neutral',
  IN_PROGRESS: 'pending',
  COMPLETED: 'approved',
  PARTIAL: 'partial',
  REJECTED: 'rejected',
}

type GapProgressBadgeProps = {
  progress: GapProgress
}

export function GapProgressBadge({ progress }: GapProgressBadgeProps) {
  return <StatusBadge tone={GAP_PROGRESS_TONES[progress]} label={gapProgressLabels[progress]} />
}
