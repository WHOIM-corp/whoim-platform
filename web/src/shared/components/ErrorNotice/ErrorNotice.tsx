import { CircleAlert } from 'lucide-react'
import styles from '@/shared/components/ErrorNotice/ErrorNotice.module.scss'
import { Button } from '@/shared/components/Button/Button.tsx'

type ErrorNoticeProps = {
  title?: string
  message: string
  actionLabel?: string
  onAction?: () => void
  compact?: boolean
}

export function ErrorNotice({ title, message, actionLabel, onAction, compact }: ErrorNoticeProps) {
  const className = compact ? `${styles.notice} ${styles.compact}` : styles.notice
  return (
    <div className={className} role="alert">
      <CircleAlert className={styles.icon} size={16} strokeWidth={1.5} aria-hidden="true" />
      <div className={styles.body}>
        {title && <strong className={styles.title}>{title}</strong>}
        <span>{message}</span>
      </div>
      {actionLabel && onAction && (
        <Button className={styles.action} onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  )
}
