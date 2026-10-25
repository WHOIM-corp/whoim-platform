import type { ReactNode } from 'react'
import { TrafficLights } from '@/layouts/components/TrafficLights/TrafficLights.tsx'
import { WindowFrame } from '@/layouts/components/WindowFrame/WindowFrame.tsx'
import styles from '@/layouts/AuthLayout/AuthLayout.module.scss'

type AuthLayoutProps = {
  intro: ReactNode
  children: ReactNode
}

export function AuthLayout({ intro, children }: AuthLayoutProps) {
  return (
    <WindowFrame>
      <div className={styles.grid}>
        <span className={styles.lights}>
          <TrafficLights />
        </span>
        {intro}
        <main className={styles.auth}>{children}</main>
      </div>
    </WindowFrame>
  )
}
