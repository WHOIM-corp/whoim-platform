import type { GapProgress, GapType } from '@/features/gap/model/gapTypes.ts'

export const gapTypeLabels: Record<GapType, string> = {
  TRIGGER_GAP: '상황 공백',
  ROOT_CAUSE_GAP: '원인 공백',
  ASSESSMENT_GAP: '판단 공백',
  CHOICE_GAP: '선택 공백',
  EXCEPTION_EXPOSURE_GAP: '예외·위험 공백',
  EVIDENCE_CONFLICT: '근거 충돌',
  UNDOCUMENTED_PATTERN: '문서에 없는 패턴',
}

export const gapProgressLabels: Record<GapProgress, string> = {
  NOT_STARTED: '질문 전',
  IN_PROGRESS: '진행 중',
  COMPLETED: '확인 완료',
  PARTIAL: '일부만 확인',
  REJECTED: '반려',
}
