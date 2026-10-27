import { CARD_STATUS_META } from '@/features/card/model/cardStatusMeta.ts'
import type { CardStateHistory } from '@/features/card/model/cardTypes.ts'
import { formatDateTime } from '@/shared/lib/formatDate.ts'
import styles from '@/features/card/components/HistoryTimeline/HistoryTimeline.module.scss'

type HistoryTimelineProps = {
  history: CardStateHistory[]
}

function transitionLabel(entry: CardStateHistory): string {
  const to = CARD_STATUS_META[entry.toStatus].label
  return entry.fromStatus === null ? to : `${CARD_STATUS_META[entry.fromStatus].label} → ${to}`
}

export function HistoryTimeline({ history }: HistoryTimelineProps) {
  if (history.length === 0) {
    return <p className={styles.empty}>기록된 상태 변경이 없습니다</p>
  }
  return (
    <ol className={styles.timeline}>
      {history.map((entry) => (
        <li key={`${entry.createdAt}-${entry.toStatus}`} className={styles.entry}>
          <span className={styles.dot} aria-hidden="true" />
          <div>
            <div className={styles.top}>
              <b>{entry.actorName ?? entry.actorUsername ?? '시스템'}</b>
              <time dateTime={entry.createdAt}>{formatDateTime(entry.createdAt)}</time>
            </div>
            <div className={styles.move}>{transitionLabel(entry)}</div>
            {entry.reason !== null && <p className={styles.reason}>{entry.reason}</p>}
          </div>
        </li>
      ))}
    </ol>
  )
}
