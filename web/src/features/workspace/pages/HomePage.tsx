import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { PredecessorHome } from '@/features/workspace/components/PredecessorHome/PredecessorHome.tsx'
import { ReviewerHome } from '@/features/workspace/components/ReviewerHome/ReviewerHome.tsx'
import { SuccessorHome } from '@/features/workspace/components/SuccessorHome/SuccessorHome.tsx'

// 직책에 맞는 홈을 고르기만 한다.
export function HomePage() {
  const member = useAuthStore((state) => state.member)
  if (member?.position === 'REVIEWER') {
    return <ReviewerHome name={member.name} />
  }
  if (member?.position === 'PREDECESSOR') {
    return <PredecessorHome name={member.name} />
  }
  if (member?.position === 'SUCCESSOR') {
    return <SuccessorHome name={member.name} />
  }
  // 관리자는 AdminGate가 먼저 안내 화면으로 보낸다.
  return null
}
