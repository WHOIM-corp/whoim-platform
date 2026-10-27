import { ForbiddenPage } from '@/features/system/pages/ForbiddenPage.tsx'
import { NotFoundPage } from '@/features/system/pages/NotFoundPage.tsx'
import type { ApiError } from '@/shared/api/apiError.ts'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'

type QueryErrorViewProps = {
  error: ApiError
  title: string
  onRetry: () => void
}

// 상세 라우트의 대상 조회 실패를 보여준다. 403·404는 권한·없음 화면이고, 그 밖에는 다시 시도할 수 있다.
export function QueryErrorView({ error, title, onRetry }: QueryErrorViewProps) {
  if (error.status === 403) {
    return <ForbiddenPage />
  }
  if (error.status === 404) {
    return <NotFoundPage />
  }
  return <ErrorNotice title={title} message={error.message} actionLabel="다시 시도" onAction={onRetry} />
}
