import type { ReactNode } from 'react'
import styles from '@/layouts/components/WindowFrame/WindowFrame.module.scss'

type WindowFrameProps = {
  children: ReactNode
}

export function WindowFrame({ children }: WindowFrameProps) {
  return (
    <div className={styles.desktop}>
      <div className={styles.window}>{children}</div>
    </div>
  )
}
