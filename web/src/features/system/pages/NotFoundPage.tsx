import { FileQuestionMark } from 'lucide-react'
import { Link } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { buttonClassName } from '@/shared/components/Button/buttonClassName.ts'
import { StateView } from '@/shared/components/StateView/StateView.tsx'

export function NotFoundPage() {
  return (
    <StateView
      icon={FileQuestionMark}
      title="페이지를 찾을 수 없습니다"
      message="주소가 바뀌었거나 존재하지 않는 화면입니다."
      action={
        <Link to={ROUTE_PATHS.home} className={buttonClassName('secondary')}>
          홈으로
        </Link>
      }
    />
  )
}
