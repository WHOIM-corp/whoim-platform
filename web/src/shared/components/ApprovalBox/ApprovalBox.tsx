import { Check, Clock, X } from 'lucide-react'
import styles from '@/shared/components/ApprovalBox/ApprovalBox.module.scss'

export type ApprovalResult =
  | { kind: 'waiting' }
  | { kind: 'approved' | 'rejected'; approverName?: string; date?: string; dateTime?: string }

type ApprovalBoxProps = {
  result: ApprovalResult
}

const RESULT_LABELS = { waiting: '대기', approved: '승인', rejected: '반려' } as const
const RESULT_ICONS = { waiting: Clock, approved: Check, rejected: X } as const

// 결재란 형태의 승인 결과 표시다.
export function ApprovalBox({ result }: ApprovalBoxProps) {
  const Icon = RESULT_ICONS[result.kind]
  return (
    <div className={`${styles.approval} ${styles[result.kind]}`}>
      <div className={styles.head}>승인자</div>
      <div className={styles.body}>
        <span className={styles.result}>
          <Icon size={14} strokeWidth={2.25} aria-hidden="true" />
          {RESULT_LABELS[result.kind]}
        </span>
        {result.kind !== 'waiting' && result.approverName && (
          <span className={styles.name}>{result.approverName}</span>
        )}
        {result.kind !== 'waiting' && result.date && (
          <time className={styles.date} dateTime={result.dateTime}>
            {result.date}
          </time>
        )}
      </div>
    </div>
  )
}
