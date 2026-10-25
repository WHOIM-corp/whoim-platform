import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import styles from '@/shared/components/StateView/StateView.module.scss'

type StateViewProps = {
  icon: LucideIcon
  title: string
  message: string
  action?: ReactNode
}

export function StateView({ icon: Icon, title, message, action }: StateViewProps) {
  return (
    <section className={styles.stateView}>
      <span className={styles.iconChip}>
        <Icon size={24} aria-hidden="true" />
      </span>
      <h1 className={styles.title}>{title}</h1>
      <p className={styles.message}>{message}</p>
      {action && <div className={styles.action}>{action}</div>}
    </section>
  )
}
