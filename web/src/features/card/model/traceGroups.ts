import type { TraceElement } from '@/features/card/model/cardTypes.ts'

export type TraceFieldKey =
  | 'situation'
  | 'signal'
  | 'cause'
  | 'condition'
  | 'judgment'
  | 'alternative'
  | 'action'
  | 'reason'
  | 'exception'
  | 'risk'

export type TraceLetter = 'T' | 'R' | 'A' | 'C' | 'E'

export type TraceGroup = {
  letter: TraceLetter
  label: string
  english: string
  fields: readonly [TraceFieldKey, TraceFieldKey]
  elements: readonly [TraceElement, TraceElement]
}

export type TraceFields = Record<TraceFieldKey, string | null>

export const TRACE_FIELD_LABELS: Record<TraceFieldKey, string> = {
  situation: '상황',
  signal: '신호',
  cause: '원인',
  condition: '조건',
  judgment: '판단',
  alternative: '대안',
  action: '행동',
  reason: '이유',
  exception: '예외',
  risk: '위험',
}

export const TRACE_GROUPS: readonly TraceGroup[] = [
  { letter: 'T', label: '상황', english: 'Trigger', fields: ['situation', 'signal'], elements: ['SITUATION', 'SIGNAL'] },
  { letter: 'R', label: '원인', english: 'Root cause', fields: ['cause', 'condition'], elements: ['CAUSE', 'CONDITION'] },
  { letter: 'A', label: '판단', english: 'Assessment', fields: ['judgment', 'alternative'], elements: ['JUDGMENT', 'ALTERNATIVE'] },
  { letter: 'C', label: '선택', english: 'Choice', fields: ['action', 'reason'], elements: ['ACTION', 'REASON'] },
  { letter: 'E', label: '예외와 위험', english: 'Exception', fields: ['exception', 'risk'], elements: ['EXCEPTION', 'RISK'] },
]

// 값이 있는 항목만 소제목과 함께 돌려준다. 모두 비면 빈 배열이다.
export function groupLines(card: TraceFields, group: TraceGroup): { label: string; text: string }[] {
  return group.fields.flatMap((field) => {
    const text = card[field]?.trim()
    return text ? [{ label: TRACE_FIELD_LABELS[field], text }] : []
  })
}

// 항목이 하나도 채워지지 않은 그룹의 수다.
export function emptyGroupCount(card: TraceFields): number {
  return TRACE_GROUPS.filter((group) => groupLines(card, group).length === 0).length
}
