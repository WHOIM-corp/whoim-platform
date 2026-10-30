import type { Position } from '@/features/auth/model/authTypes.ts'
import { canUploadEvidence } from '@/features/evidence/model/evidenceUploadRule.ts'
import type { HandoverStatus } from '@/features/handover/model/handoverTypes.ts'

export type EvidenceLockReason = 'ANALYSIS_STARTED' | 'PUBLISHED' | 'REVIEWER_READ_ONLY'

// 업로드할 수 있으면 null, 잠겨 있으면 잠금 사유를 돌려준다.
export function evidenceLockReason(params: {
  position: Position | null
  status: HandoverStatus
  gapCount: number
}): EvidenceLockReason | null {
  if (params.position !== 'PREDECESSOR') {
    return 'REVIEWER_READ_ONLY'
  }
  if (canUploadEvidence({ status: params.status, gapCount: params.gapCount })) {
    return null
  }
  return params.status === 'PUBLISHED' ? 'PUBLISHED' : 'ANALYSIS_STARTED'
}
