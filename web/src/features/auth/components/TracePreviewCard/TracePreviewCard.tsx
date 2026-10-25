import { Check } from 'lucide-react'
import { TRACE_PREVIEW_ROWS } from '@/features/auth/components/TracePreviewCard/tracePreviewRows.ts'
import styles from '@/features/auth/components/TracePreviewCard/TracePreviewCard.module.scss'

export function TracePreviewCard() {
  return (
    <article className={styles.preview} aria-hidden="true">
      <div className={styles.face}>
        <div className={styles.head}>
          <span className={styles.mono}>TRACE-0012</span>
          <span className={styles.badge}>
            <Check size={10} strokeWidth={2} />
            승인됨
          </span>
        </div>
        <h3 className={styles.title}>정산 배치가 실패하면 언제 재처리할까</h3>
        <ol className={styles.rows}>
          {TRACE_PREVIEW_ROWS.map((row) => (
            <li key={row.letter} className={styles.row}>
              <span className={styles.letter}>{row.letter}</span>
              <span>
                {row.text}
                <span className={styles.mark}>{row.evidenceNumber}</span>
              </span>
            </li>
          ))}
        </ol>
        <div className={styles.foot}>
          <span>근거 10건</span>
          <span>전임자 확인 · 승인</span>
        </div>
      </div>
    </article>
  )
}
