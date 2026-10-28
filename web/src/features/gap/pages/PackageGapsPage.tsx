import { useParams } from 'react-router'
import { AnalysisStart } from '@/features/gap/components/AnalysisStart/AnalysisStart.tsx'
import { GapGrid } from '@/features/gap/components/GapGrid/GapGrid.tsx'
import { PackageTabs } from '@/features/gap/components/PackageTabs/PackageTabs.tsx'
import { useGaps } from '@/features/gap/hooks/useGaps.ts'
import { PackageStatusBadge } from '@/features/handover/components/PackageStatusBadge/PackageStatusBadge.tsx'
import { useHandoverPackage } from '@/features/handover/hooks/useHandoverPackage.ts'
import { QueryErrorView } from '@/features/system/components/QueryErrorView/QueryErrorView.tsx'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'

export function PackageGapsPage() {
  const { packageId = '' } = useParams()
  const packageQuery = useHandoverPackage(packageId)
  const gapsQuery = useGaps(packageId)

  if (packageQuery.error !== null) {
    return (
      <QueryErrorView error={packageQuery.error} title="Package를 불러오지 못했습니다" onRetry={packageQuery.refetch} />
    )
  }
  const handoverPackage = packageQuery.data

  function renderBody() {
    if (gapsQuery.isLoading) {
      return <CardSkeletonGrid />
    }
    if (gapsQuery.error !== null) {
      return (
        <ErrorNotice
          title="Gap을 불러오지 못했습니다"
          message={gapsQuery.error.message}
          actionLabel="다시 시도"
          onAction={gapsQuery.refetch}
        />
      )
    }
    const gaps = gapsQuery.data
    if (gaps === undefined || gaps.length === 0) {
      return <AnalysisStart packageId={packageId} onStale={gapsQuery.refetch} />
    }
    return <GapGrid packageId={packageId} gaps={gaps} onStale={gapsQuery.refetch} />
  }

  return (
    <>
      <PageHeader
        title={handoverPackage?.name ?? 'Package'}
        meta={handoverPackage && <PackageStatusBadge status={handoverPackage.status} />}
      />
      <PackageTabs packageId={packageId} active="gaps" />
      {renderBody()}
    </>
  )
}
