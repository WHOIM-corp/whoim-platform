import type { ReactNode } from 'react'
import styles from '@/shared/components/GlassPanel/GlassPanel.module.scss'

type GlassPanelProps = {
  title: string
  subtitle?: string
  actions?: ReactNode
  children: ReactNode
}

export function GlassPanel({ title, subtitle, actions, children }: GlassPanelProps) {
  return (
    <aside className={styles.panel}>
      <header className={styles.header}>
        <div>
          <h2 className={styles.title}>{title}</h2>
          {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
        </div>
        {actions && <div className={styles.actions}>{actions}</div>}
      </header>
      <div className={styles.body}>{children}</div>
    </aside>
  )
}
