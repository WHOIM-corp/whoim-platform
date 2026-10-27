import type { TraceGroup } from '@/features/card/model/traceGroups.ts'
import type { Interview } from '@/features/interview/model/interviewTypes.ts'

// 서버 InterviewEndPolicy.MAX_ANSWERED_TURNS와 같은 값이다. 응답에 없어 진행 표시에만 쓰고, 종료 판단은 서버 status가 한다.
export const INTERVIEW_MAX_ANSWERED_TURNS = 5

export type GroupFillStatus = 'filled' | 'partial' | 'unknown' | 'waiting'

// 그룹의 두 항목이 모두 채워지면 filled, 하나만이면 partial, 채운 것 없이 모르겠다는 답만 있으면 unknown이다.
export function groupFillStatus(interview: Interview, group: TraceGroup): GroupFillStatus {
  const statuses = group.elements.map((element) => interview.traceState[element]?.status)
  const filledCount = statuses.filter((status) => status === 'FILLED').length
  if (filledCount === group.elements.length) {
    return 'filled'
  }
  if (filledCount > 0) {
    return 'partial'
  }
  return statuses.includes('UNKNOWN') ? 'unknown' : 'waiting'
}

export function answeredTurnCount(interview: Interview): number {
  return interview.turns.filter((turn) => turn.answer !== null).length
}

// 답변은 저장됐지만 AI가 처리하지 못한 상태(503 뒤)다. 다시 불러와도 이 상태로 남아 재시도가 필요하다.
export function hasUnprocessedAnswer(interview: Interview): boolean {
  const lastTurn = interview.turns.at(-1)
  return (
    interview.status === 'IN_PROGRESS' &&
    interview.currentQuestion === null &&
    lastTurn !== undefined &&
    lastTurn.answer !== null &&
    !lastTurn.processed
  )
}
