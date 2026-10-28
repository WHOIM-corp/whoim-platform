import type { LucideIcon } from 'lucide-react'
import styles from '@/shared/components/StatusBadge/StatusBadge.module.scss'

export type StatusBadgeTone = 'neutral' | 'pending' | 'approved' | 'rejected' | 'published' | 'partial'

type StatusBadgeProps = {
  tone: StatusBadgeTone
  icon?: LucideIcon
  label: string
}

export function StatusBadge({ tone, icon: Icon, label }: StatusBadgeProps) {
  return (
    <span className={`${styles.badge} ${styles[tone]}`}>
      {Icon && <Icon size={12} strokeWidth={2} aria-hidden="true" />}
      {label}
    </span>
  )
}
