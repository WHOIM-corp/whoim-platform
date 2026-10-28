import { Globe } from 'lucide-react'
import { useState } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { PackageTabs } from '@/features/gap/components/PackageTabs/PackageTabs.tsx'
import { CardGrid } from '@/features/card/components/CardGrid/CardGrid.tsx'
import { CARD_QUEUE_BY_POSITION } from '@/features/card/model/cardQueue.ts'
import { closedReasonFor } from '@/features/card/model/cardOpenRule.ts'
import { filterByStatus, parseCardStatusFilter, type CardStatusFilter } from '@/features/card/model/cardStatusFilter.ts'
import { PackageProgress } from '@/features/handover/components/PackageProgress/PackageProgress.tsx'
import { PackageStatusBadge } from '@/features/handover/components/PackageStatusBadge/PackageStatusBadge.tsx'
import { PublishDialog } from '@/features/handover/components/PublishDialog/PublishDialog.tsx'
import { useHandoverPackage } from '@/features/handover/hooks/useHandoverPackage.ts'
import { countCardsWithStatus } from '@/features/handover/model/packageCardCounts.ts'
import { canPublish, publishBlockReason } from '@/features/handover/model/publishRule.ts'
import { QueryErrorView } from '@/features/system/components/QueryErrorView/QueryErrorView.tsx'
import { Button } from '@/shared/components/Button/Button.tsx'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'
import { SegmentedControl } from '@/shared/components/SegmentedControl/SegmentedControl.tsx'
import styles from '@/features/handover/pages/PackageDetailPage.module.scss'

const STATUS_QUERY_KEY = 'status'

export function PackageDetailPage() {
  const { packageId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const position = useAuthStore((state) => state.member?.position ?? null)
  const packageQuery = useHandoverPackage(packageId)
  const [publishing, setPublishing] = useState(false)
  const requestedFilter = parseCardStatusFilter(searchParams.get(STATUS_QUERY_KEY))

  function changeFilter(next: CardStatusFilter) {
    const nextParams = new URLSearchParams(searchParams)
    if (next === 'ALL') {
      nextParams.delete(STATUS_QUERY_KEY)
    } else {
      nextParams.set(STATUS_QUERY_KEY, next)
    }
    setSearchParams(nextParams, { replace: true })
  }

  if (packageQuery.isLoading) {
    return <CardSkeletonGrid />
  }
  if (packageQuery.error !== null) {
    return (
      <QueryErrorView error={packageQuery.error} title="Package를 불러오지 못했습니다" onRetry={packageQuery.refetch} />
    )
  }
  const handoverPackage = packageQuery.data
  if (handoverPackage === undefined) {
    return null
  }

  const isReviewer = position === 'REVIEWER'
  const isSuccessor = position === 'SUCCESSOR'
  const canManagePublish = isReviewer && handoverPackage.status === 'WRITING'
  const blockReason = publishBlockReason(handoverPackage)
  const queue = position === null ? undefined : CARD_QUEUE_BY_POSITION[position]
  const filterItems: { value: CardStatusFilter; label: string; count: number }[] = [
    { value: 'ALL', label: '전체', count: handoverPackage.cards.length },
    ...(queue
      ? [{ value: queue.status, label: queue.label, count: countCardsWithStatus(handoverPackage, queue.status) }]
      : []),
    { value: 'APPROVED', label: '승인됨', count: countCardsWithStatus(handoverPackage, 'APPROVED') },
    { value: 'REJECTED', label: '반려됨', count: countCardsWithStatus(handoverPackage, 'REJECTED') },
  ]
  // 이 직책의 필터에 없는 값(예: 승인자의 DRAFT)은 전체로 본다.
  const filter = filterItems.some((item) => item.value === requestedFilter) ? requestedFilter : 'ALL'

  return (
    <>
      <PageHeader
        title={handoverPackage.name}
        meta={
          <>
            <PackageStatusBadge status={handoverPackage.status} />
            <span>전임자 {handoverPackage.predecessor.name}</span>
            {!isSuccessor && <span>후임자 {handoverPackage.successor?.name ?? '미지정'}</span>}
          </>
        }
        actions={
          canManagePublish && (
            <Button
              variant="primary"
              disabled={!canPublish(handoverPackage)}
              title={blockReason ?? undefined}
              onClick={() => setPublishing(true)}
            >
              <Globe size={16} aria-hidden="true" />
              Package 공개
            </Button>
          )
        }
      />
      {position === 'PREDECESSOR' && <PackageTabs packageId={packageId} active="cards" />}
      {!isSuccessor && (
        <>
          <PackageProgress handoverPackage={handoverPackage} blockReason={canManagePublish ? blockReason : null} />
          <div className={styles.toolrow}>
            <SegmentedControl items={filterItems} value={filter} onChange={changeFilter} ariaLabel="Card 상태 필터" />
          </div>
        </>
      )}
      <CardGrid
        cards={filterByStatus(handoverPackage.cards, filter)}
        closedReasonOf={(card) => closedReasonFor(position, card.status)}
      />
      {publishing && (
        <PublishDialog
          handoverPackage={handoverPackage}
          onClose={() => setPublishing(false)}
          onFailure={packageQuery.refetch}
        />
      )}
    </>
  )
}
