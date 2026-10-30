import { Lock } from 'lucide-react'
import type { EvidenceLockReason } from '@/features/evidence/model/evidenceAccess.ts'
import styles from '@/features/evidence/components/EvidenceLockNotice/EvidenceLockNotice.module.scss'

type EvidenceLockNoticeProps = {
  reason: EvidenceLockReason
}

const LOCK_COPY: Record<EvidenceLockReason, { title: string; message: string }> = {
  ANALYSIS_STARTED: {
    title: 'Gap 분석이 시작되어 문서를 추가할 수 없습니다',
    message: '업로드한 문서는 더 이상 추가하거나 바꿀 수 없습니다. 아래 목록은 읽기 전용입니다.',
  },
  PUBLISHED: {
    title: '공개된 Package입니다',
    message: 'PUBLISHED 상태의 Package는 근거 자료를 추가할 수 없습니다. 아래 목록은 읽기 전용입니다.',
  },
  REVIEWER_READ_ONLY: {
    title: '읽기 전용',
    message: '승인자는 업로드된 근거 자료를 확인만 할 수 있습니다.',
  },
}

export function EvidenceLockNotice({ reason }: EvidenceLockNoticeProps) {
  const copy = LOCK_COPY[reason]
  return (
    <div className={styles.lock} role="status">
      <span className={styles.iconChip}>
        <Lock size={14} aria-hidden="true" />
      </span>
      <p className={styles.text}>
        <strong className={styles.title}>{copy.title}</strong>
        <span>{copy.message}</span>
      </p>
    </div>
  )
}
