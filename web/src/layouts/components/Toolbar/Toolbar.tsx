import type { ReactNode } from 'react'
import styles from '@/layouts/components/Toolbar/Toolbar.module.scss'

type ToolbarProps = {
  children: ReactNode
}

export function Toolbar({ children }: ToolbarProps) {
  return <header className={styles.toolbar}>{children}</header>
}
