export type TracePreviewRow = { letter: string; text: string; evidenceNumber: number }

export const TRACE_PREVIEW_ROWS: TracePreviewRow[] = [
  { letter: 'T', text: '실패 건수가 3% 미만일 때', evidenceNumber: 1 },
  { letter: 'R', text: '결제사 응답 지연이 대부분', evidenceNumber: 2 },
  { letter: 'A', text: '점검 시간과 겹치면 재실패', evidenceNumber: 3 },
  { letter: 'C', text: '03:30 이후 실패 건만 재처리', evidenceNumber: 4 },
  { letter: 'E', text: '월말·10% 이상은 즉시 보고', evidenceNumber: 5 },
]
