import { BadgeCheck, ClipboardCheck, FileSearch, MessagesSquare, UsersRound } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export type ServiceFlowStep = { icon: LucideIcon; title: string; description: string }

export const SERVICE_FLOW_STEPS: ServiceFlowStep[] = [
  {
    icon: FileSearch,
    title: '업무 기록 분석',
    description: '기록에는 있지만 이유가 빠진 판단을 찾습니다',
  },
  {
    icon: MessagesSquare,
    title: '전임자 AI 인터뷰',
    description: '상황, 원인, 판단, 선택, 예외를 묻습니다',
  },
  {
    icon: ClipboardCheck,
    title: 'TRACE Card 생성',
    description: '발언과 문서 근거를 항목마다 연결합니다',
  },
  {
    icon: BadgeCheck,
    title: '전임자 확인과 승인',
    description: '두 단계 검수를 거친 Card만 공개됩니다',
  },
  {
    icon: UsersRound,
    title: '후임자 상담',
    description: '공개된 Card를 근거로 판단을 돕습니다',
  },
]
