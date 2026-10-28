import { FileText } from 'lucide-react'
import type { GapAnchor } from '@/features/gap/model/gapTypes.ts'
import styles from '@/features/interview/components/GapEvidence/GapEvidence.module.scss'

type GapEvidenceProps = {
  anchors: GapAnchor[]
}

// Gap 분석이 찾은 문서 근거(파일과 발췌)다.
export function GapEvidence({ anchors }: GapEvidenceProps) {
  return (
    <section className={styles.section} aria-label="AI가 찾은 근거">
      <h3 className={styles.heading}>AI가 찾은 근거</h3>
      {anchors.length === 0 ? (
        <p className={styles.empty}>연결된 문서 근거가 없습니다</p>
      ) : (
        <ul className={styles.list}>
          {anchors.map((anchor) => (
            <li key={`${anchor.documentId}-${anchor.startOffset}`} className={styles.source}>
              <span className={styles.file}>
                <FileText size={14} strokeWidth={2} aria-hidden="true" />
                <span className={styles.fileName}>{anchor.fileName}</span>
                <span className={styles.offset}>
                  {anchor.startOffset}-{anchor.endOffset}
                </span>
              </span>
              <q className={styles.quote}>{anchor.excerpt}</q>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
