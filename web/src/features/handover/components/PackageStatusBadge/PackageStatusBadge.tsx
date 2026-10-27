import { HANDOVER_STATUS_META } from '@/features/handover/model/handoverStatusMeta.ts'
import type { HandoverStatus } from '@/features/handover/model/handoverTypes.ts'
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge.tsx'

type PackageStatusBadgeProps = {
  status: HandoverStatus
}

export function PackageStatusBadge({ status }: PackageStatusBadgeProps) {
  const { label, tone, icon } = HANDOVER_STATUS_META[status]
  return <StatusBadge tone={tone} icon={icon} label={label} />
}
