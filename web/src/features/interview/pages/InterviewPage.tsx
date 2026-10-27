import { generatePath, Navigate, useParams } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { GapProgressBadge } from '@/features/gap/components/GapProgressBadge/GapProgressBadge.tsx'
import { useGaps } from '@/features/gap/hooks/useGaps.ts'
import { InterviewSession } from '@/features/interview/components/InterviewSession/InterviewSession.tsx'
import { QueryErrorView } from '@/features/system/components/QueryErrorView/QueryErrorView.tsx'
import { NotFoundPage } from '@/features/system/pages/NotFoundPage.tsx'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'

// 인터뷰 시작은 Gap 카드에서 한다. 인터뷰가 없거나 반려된 Gap은 Gap 목록으로 돌려보낸다.
export function InterviewPage() {
  const { packageId = '', gapId = '' } = useParams()
  const gapsQuery = useGaps(packageId)

  if (gapsQuery.isLoading) {
    return <CardSkeletonGrid count={1} />
  }
  if (gapsQuery.error !== null) {
    return (
      <QueryErrorView
        error={gapsQuery.error}
        title="Gap을 불러오지 못했습니다"
        onRetry={gapsQuery.refetch}
      />
    )
  }
  const gap = gapsQuery.data?.find((candidate) => candidate.id === gapId)
  if (gap === undefined) {
    return <NotFoundPage />
  }
  if (gap.interviewId === null || gap.progress === 'REJECTED') {
    return <Navigate to={generatePath(ROUTE_PATHS.packageGaps, { packageId })} replace />
  }

  return (
    <>
      <PageHeader title={gap.summary} meta={<GapProgressBadge progress={gap.progress} />} />
      <InterviewSession
        key={gap.interviewId}
        gap={gap}
        interviewId={gap.interviewId}
        onGapsStale={gapsQuery.refetch}
      />
    </>
  )
}
