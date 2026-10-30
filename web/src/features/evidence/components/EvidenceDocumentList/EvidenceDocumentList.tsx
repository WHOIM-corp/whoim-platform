import { FileText } from 'lucide-react'
import { DOCUMENT_TYPE_LABELS } from '@/features/evidence/model/documentTypeLabels.ts'
import type { EvidenceDocument } from '@/features/evidence/model/evidenceTypes.ts'
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge.tsx'
import { formatDateTime } from '@/shared/lib/formatDate.ts'
import styles from '@/features/evidence/components/EvidenceDocumentList/EvidenceDocumentList.module.scss'

type EvidenceDocumentListProps = {
  documents: EvidenceDocument[]
  canUpload: boolean
}

const EMPTY_MESSAGE_UPLOADABLE = '파일을 올리면 여기에 표시됩니다.'
const EMPTY_MESSAGE_READ_ONLY = '이 Package에는 근거 자료가 없습니다.'

export function EvidenceDocumentList({ documents, canUpload }: EvidenceDocumentListProps) {
  return (
    <section className={styles.section} aria-labelledby="evidence-list-heading">
      <header className={styles.header}>
        <h2 id="evidence-list-heading" className={styles.heading}>
          올라온 문서
        </h2>
        <span className={styles.count}>{documents.length}건</span>
      </header>
      {documents.length === 0 ? (
        <div className={styles.empty}>
          <span className={styles.emptyIcon}>
            <FileText size={20} aria-hidden="true" />
          </span>
          <p className={styles.emptyTitle}>올라온 문서가 없습니다</p>
          <p className={styles.emptyMessage}>{canUpload ? EMPTY_MESSAGE_UPLOADABLE : EMPTY_MESSAGE_READ_ONLY}</p>
        </div>
      ) : (
        <>
          <div className={`${styles.row} ${styles.head}`} aria-hidden="true">
            <span>파일명</span>
            <span>종류</span>
            <span className={styles.number}>글자 수</span>
            <span>올린 시각</span>
          </div>
          <ul className={styles.list}>
            {documents.map((evidenceDocument) => (
              <li key={evidenceDocument.id} className={styles.row}>
                <span className={styles.fileName}>
                  <FileText size={16} aria-hidden="true" />
                  <span className={styles.name}>{evidenceDocument.fileName}</span>
                </span>
                <StatusBadge tone="neutral" label={DOCUMENT_TYPE_LABELS[evidenceDocument.documentType]} />
                <span className={styles.number}>{evidenceDocument.characterCount.toLocaleString('ko-KR')}자</span>
                <span className={styles.when}>{formatDateTime(evidenceDocument.uploadedAt)}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
