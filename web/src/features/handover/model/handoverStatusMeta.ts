import { Globe, PencilLine, type LucideIcon } from 'lucide-react'
import type { HandoverStatus } from '@/features/handover/model/handoverTypes.ts'
import type { StatusBadgeTone } from '@/shared/components/StatusBadge/StatusBadge.tsx'

export const HANDOVER_STATUS_META: Record<HandoverStatus, { label: string; tone: StatusBadgeTone; icon: LucideIcon }> = {
  WRITING: { label: '작성 중', tone: 'neutral', icon: PencilLine },
  PUBLISHED: { label: '공개됨', tone: 'published', icon: Globe },
}
