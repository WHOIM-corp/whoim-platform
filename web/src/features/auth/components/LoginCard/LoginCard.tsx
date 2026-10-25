import type { ReactNode } from 'react'
import styles from '@/features/auth/components/LoginCard/LoginCard.module.scss'

type LoginCardProps = {
  notice?: ReactNode
  children: ReactNode
}

export function LoginCard({ notice, children }: LoginCardProps) {
  return (
    <div className={styles.card}>
      <div className={styles.face}>
        <h2 className={styles.title}>로그인</h2>
        <p className={styles.subtitle}>업무 계정으로 로그인해 주세요</p>
        {notice && <div className={styles.notice}>{notice}</div>}
        {children}
      </div>
    </div>
  )
}
