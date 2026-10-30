import type { HandoverStatus } from '@/features/handover/model/handoverTypes.ts'

export const MAX_EVIDENCE_FILE_BYTES = 131072
export const ALLOWED_EVIDENCE_EXTENSIONS = ['md', 'txt']

const BYTES_PER_KILOBYTE = 1024
const MAX_EVIDENCE_FILE_KILOBYTES = MAX_EVIDENCE_FILE_BYTES / BYTES_PER_KILOBYTE

export const EVIDENCE_ACCEPT_ATTRIBUTE = ALLOWED_EVIDENCE_EXTENSIONS.map((extension) => `.${extension}`).join(',')
export const EVIDENCE_LIMIT_HINT = `${ALLOWED_EVIDENCE_EXTENSIONS.map((extension) => `.${extension}`).join(', ')} · 파일당 최대 ${MAX_EVIDENCE_FILE_KILOBYTES}KB`

export function formatFileSize(bytes: number): string {
  return `${(bytes / BYTES_PER_KILOBYTE).toFixed(1)}KB`
}

function extensionOf(fileName: string): string | null {
  const dotIndex = fileName.lastIndexOf('.')
  return dotIndex > 0 ? fileName.slice(dotIndex + 1).toLowerCase() : null
}

export function validateEvidenceFile(file: File): string | null {
  if (file.size === 0) {
    return '빈 파일은 올릴 수 없습니다.'
  }
  const extension = extensionOf(file.name)
  if (extension === null || !ALLOWED_EVIDENCE_EXTENSIONS.includes(extension)) {
    return `${ALLOWED_EVIDENCE_EXTENSIONS.map((allowed) => `.${allowed}`).join(', ')} 파일만 올릴 수 있습니다.`
  }
  if (file.size > MAX_EVIDENCE_FILE_BYTES) {
    return `파일 크기는 ${MAX_EVIDENCE_FILE_KILOBYTES}KB 이하여야 합니다.`
  }
  return null
}

export function canUploadEvidence(params: { status: HandoverStatus; gapCount: number }): boolean {
  return params.status === 'WRITING' && params.gapCount === 0
}
