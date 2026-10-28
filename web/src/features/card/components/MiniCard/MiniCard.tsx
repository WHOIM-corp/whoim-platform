import { CardStatusBadge } from '@/features/card/components/CardStatusBadge/CardStatusBadge.tsx'
import type { CardSummary } from '@/features/card/model/cardTypes.ts'
import type { HandoverCardSummary } from '@/features/handover/model/handoverTypes.ts'
import { formatShortDate } from '@/shared/lib/formatDate.ts'
import styles from '@/features/card/components/MiniCard/MiniCard.module.scss'

type MiniCardProps = {
  summary: CardSummary | HandoverCardSummary
  // 주어지면 카드 전체가 버튼이 된다.
  onOpen?: () => void
  // 주어지면 열 수 없는 카드로 그리고 이 문구를 보여준다 (예: 승인자에게 숨겨진 초안).
  disabledReason?: string
}

// 3:4 미니 Card다. 근거 개수는 서버 응답에 있으므로 추가 조회가 없다.
export function MiniCard({ summary, onOpen, disabledReason }: MiniCardProps) {
  const { directEvidenceCount, recordedEvidenceCount } = summary
  const interactive = onOpen !== undefined && disabledReason === undefined
  const total = directEvidenceCount + recordedEvidenceCount

  const face = (
    <span className={styles.face}>
      <span className={styles.head}>
        <CardStatusBadge status={summary.status} />
        <time className={styles.date} dateTime={summary.updatedAt}>{formatShortDate(summary.updatedAt)}</time>
      </span>
      <span className={styles.body}>
        <span className={styles.title}>{summary.title}</span>
        {summary.action !== null && (
          <span className={styles.gist}>
            <b>선택</b>
            {summary.action}
          </span>
        )}
        <span className={styles.meterBlock}>
          <span
            className={styles.meter}
            role="img"
            aria-label={`발언 근거 ${directEvidenceCount}건, 문서 근거 ${recordedEvidenceCount}건`}
          >
            {total === 0 ? (
              <span className={styles.none} />
            ) : (
              <>
                <span className={styles.direct} style={{ flexGrow: directEvidenceCount }} />
                <span className={styles.recorded} style={{ flexGrow: recordedEvidenceCount }} />
              </>
            )}
          </span>
          <span className={styles.legend}>
            <span>
              <i className={styles.directKey} />
              발언 {directEvidenceCount}
            </span>
            <span>
              <i className={styles.recordedKey} />
              문서 {recordedEvidenceCount}
            </span>
          </span>
        </span>
        {disabledReason !== undefined && <span className={styles.reason}>{disabledReason}</span>}
      </span>
    </span>
  )

  if (interactive) {
    return (
      <button type="button" className={`${styles.mini} ${styles.interactive}`} onClick={onOpen}>
        {face}
      </button>
    )
  }
  return <div className={styles.mini}>{face}</div>
}
