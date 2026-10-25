import { LockKeyhole } from 'lucide-react'
import { Link } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { positionLabels } from '@/features/auth/model/authTypes.ts'
import { buttonClassName } from '@/shared/components/Button/buttonClassName.ts'
import { StateView } from '@/shared/components/StateView/StateView.tsx'

export function ForbiddenPage() {
  const position = useAuthStore((state) => state.member?.position ?? null)
  const subject = position === null ? '현재' : `${positionLabels[position]} 계정으로는`
  return (
    <StateView
      icon={LockKeyhole}
      title="접근 권한이 없습니다"
      message={`${subject} 볼 수 없는 화면입니다.`}
      action={
        <Link to={ROUTE_PATHS.home} className={buttonClassName('secondary')}>
          홈으로
        </Link>
      }
    />
  )
}
