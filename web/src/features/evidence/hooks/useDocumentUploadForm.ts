import { useState } from 'react'
import type { DragEvent } from 'react'
import { useUploadEvidenceDocument } from '@/features/evidence/hooks/useUploadEvidenceDocument.ts'
import type { DocumentType } from '@/features/evidence/model/evidenceTypes.ts'
import { validateEvidenceFile } from '@/features/evidence/model/evidenceUploadRule.ts'

const DEFAULT_DOCUMENT_TYPE: DocumentType = 'RUNBOOK'

type DocumentUploadFormState = {
  file: File | null
  documentType: DocumentType
  isDragOver: boolean
  isPending: boolean
  // 사전 검증 오류와 서버 오류 중 화면에 보일 안내 문구 하나다.
  errorMessage: string | null
  setDocumentType: (documentType: DocumentType) => void
  selectFile: (file: File | null) => void
  submit: () => void
  dropZoneHandlers: {
    onDragOver: (event: DragEvent) => void
    onDragLeave: (event: DragEvent) => void
    onDrop: (event: DragEvent) => void
  }
}

export function useDocumentUploadForm(params: {
  packageId: string
  onStale: () => void
  // 파일 입력의 소유자는 화면이다. 값을 비워야 같은 파일을 다시 골라도 onChange가 발생한다.
  clearFileInput: () => void
}): DocumentUploadFormState {
  const upload = useUploadEvidenceDocument()
  const [file, setFile] = useState<File | null>(null)
  const [documentType, setDocumentType] = useState<DocumentType>(DEFAULT_DOCUMENT_TYPE)
  const [validationMessage, setValidationMessage] = useState<string | null>(null)
  const [isDragOver, setIsDragOver] = useState(false)

  function selectFile(selected: File | null) {
    upload.reset()
    params.clearFileInput()
    if (selected === null) {
      setFile(null)
      setValidationMessage(null)
      return
    }
    const message = validateEvidenceFile(selected)
    setValidationMessage(message)
    setFile(message === null ? selected : null)
  }

  function submit() {
    if (file === null) {
      return
    }
    upload.submit(
      { packageId: params.packageId, documentType, file },
      {
        onSuccess: () => {
          setFile(null)
          params.clearFileInput()
        },
        onError: (error) => {
          if (error.errorCode === 'STATE_CONFLICT') {
            params.onStale()
          }
        },
      },
    )
  }

  const dropZoneHandlers = {
    onDragOver: (event: DragEvent) => {
      event.preventDefault()
      setIsDragOver(true)
    },
    onDragLeave: (event: DragEvent) => {
      // 자식 요소로 옮겨 가는 이동은 영역을 벗어난 것이 아니다.
      if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
        setIsDragOver(false)
      }
    },
    onDrop: (event: DragEvent) => {
      event.preventDefault()
      setIsDragOver(false)
      if (!upload.isPending) {
        selectFile(event.dataTransfer.files.item(0))
      }
    },
  }

  return {
    file,
    documentType,
    isDragOver,
    isPending: upload.isPending,
    errorMessage: validationMessage ?? upload.error?.message ?? null,
    setDocumentType,
    selectFile,
    submit,
    dropZoneHandlers,
  }
}
