import { FileText, Upload, X } from 'lucide-react'
import { useRef } from 'react'
import { useDocumentUploadForm } from '@/features/evidence/hooks/useDocumentUploadForm.ts'
import { DOCUMENT_TYPE_OPTIONS } from '@/features/evidence/model/documentTypeLabels.ts'
import type { DocumentType } from '@/features/evidence/model/evidenceTypes.ts'
import {
  EVIDENCE_ACCEPT_ATTRIBUTE,
  EVIDENCE_LIMIT_HINT,
  formatFileSize,
} from '@/features/evidence/model/evidenceUploadRule.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import styles from '@/features/evidence/components/DocumentUploadForm/DocumentUploadForm.module.scss'

type DocumentUploadFormProps = {
  packageId: string
  // 업로드 중 분석이 시작된 Package(409)면 화면의 잠금 상태가 낡았으므로 다시 불러온다.
  onStale: () => void
}

export function DocumentUploadForm({ packageId, onStale }: DocumentUploadFormProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const form = useDocumentUploadForm({
    packageId,
    onStale,
    clearFileInput: () => {
      if (inputRef.current !== null) {
        inputRef.current.value = ''
      }
    },
  })
  const dropZoneClassName = form.isDragOver ? `${styles.dropZone} ${styles.over}` : styles.dropZone

  return (
    <section className={styles.section} aria-labelledby="evidence-upload-heading">
      <header className={styles.header}>
        <h2 id="evidence-upload-heading" className={styles.heading}>
          파일 업로드
        </h2>
      </header>
      {form.errorMessage !== null && (
        <div className={styles.notice}>
          <ErrorNotice title="업로드하지 못했습니다" message={form.errorMessage} />
        </div>
      )}
      <div className={dropZoneClassName} {...form.dropZoneHandlers}>
        <span className={styles.iconChip}>
          <Upload size={20} aria-hidden="true" />
        </span>
        <strong>파일을 끌어다 놓거나 선택하세요</strong>
        <span className={styles.hint}>{EVIDENCE_LIMIT_HINT}</span>
        <input
          ref={inputRef}
          type="file"
          accept={EVIDENCE_ACCEPT_ATTRIBUTE}
          className={styles.fileInput}
          aria-label="업로드할 파일"
          tabIndex={-1}
          onChange={(event) => form.selectFile(event.target.files?.item(0) ?? null)}
        />
        <Button className={styles.pick} onClick={() => inputRef.current?.click()} disabled={form.isPending}>
          파일 선택
        </Button>
      </div>
      {form.file !== null && (
        <div className={styles.chosen}>
          <FileText size={16} aria-hidden="true" />
          <span className={styles.chosenName}>{form.file.name}</span>
          <span className={styles.hint}>{formatFileSize(form.file.size)}</span>
          <Button
            className={styles.remove}
            onClick={() => form.selectFile(null)}
            disabled={form.isPending}
            aria-label="선택한 파일 제거"
          >
            <X size={14} aria-hidden="true" />
            제거
          </Button>
        </div>
      )}
      <div className={styles.row}>
        <div className={styles.field}>
          <label htmlFor="evidence-document-type" className={styles.label}>
            문서 종류
          </label>
          <select
            id="evidence-document-type"
            className={styles.select}
            value={form.documentType}
            disabled={form.isPending}
            onChange={(event) => form.setDocumentType(event.target.value as DocumentType)}
          >
            {DOCUMENT_TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label} ({option.value})
              </option>
            ))}
          </select>
        </div>
        <Button
          variant="primary"
          className={styles.upload}
          disabled={form.file === null}
          loading={form.isPending}
          onClick={form.submit}
        >
          업로드
        </Button>
      </div>
    </section>
  )
}
