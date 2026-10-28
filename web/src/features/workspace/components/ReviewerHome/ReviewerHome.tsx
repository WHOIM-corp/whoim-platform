import { useState } from 'react'
import { Link } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { CardGrid } from '@/features/card/components/CardGrid/CardGrid.tsx'
import { useCards } from '@/features/card/hooks/useCards.ts'
import { CARD_QUEUE_BY_POSITION } from '@/features/card/model/cardQueue.ts'
import { filterByStatus } from '@/features/card/model/cardStatusFilter.ts'
import type { CardStatus } from '@/features/card/model/cardTypes.ts'
import { PackageTable } from '@/features/handover/components/PackageTable/PackageTable.tsx'
import { useHandoverPackages } from '@/features/handover/hooks/useHandoverPackages.ts'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'
import { SegmentedControl } from '@/shared/components/SegmentedControl/SegmentedControl.tsx'
import styles from '@/features/workspace/components/ReviewerHome/ReviewerHome.module.scss'

type ReviewerHomeProps = {
  name: string
}

type HomeTab = Extract<CardStatus, 'PREDECESSOR_CONFIRMED' | 'APPROVED' | 'REJECTED'>

const HOME_TABS: { value: HomeTab; label: string }[] = [
  { value: 'PREDECESSOR_CONFIRMED', label: '검수 대기' },
  { value: 'APPROVED', label: '승인됨' },
  { value: 'REJECTED', label: '반려됨' },
]

// 승인자에게 Card 목록은 초안을 뺀 전체다. 한 번 불러와 탭별로 나눈다.
export function ReviewerHome({ name }: ReviewerHomeProps) {
  const [tab, setTab] = useState<HomeTab>('PREDECESSOR_CONFIRMED')
  const cardsQuery = useCards()
  const packagesQuery = useHandoverPackages()
  const cards = cardsQuery.data ?? []
  const tabItems = HOME_TABS.map((item) => ({ ...item, count: filterByStatus(cards, item.value).length }))

  function renderPackages() {
    if (packagesQuery.isLoading) {
      return <CardSkeletonGrid count={2} />
    }
    if (packagesQuery.error !== null) {
      return (
        <ErrorNotice
          title="Package를 불러오지 못했습니다"
          message={packagesQuery.error.message}
          actionLabel="다시 시도"
          onAction={packagesQuery.refetch}
        />
      )
    }
    const packages = packagesQuery.data ?? []
    if (packages.length === 0) {
      return <p className={styles.empty}>아직 만든 Package가 없습니다.</p>
    }
    return <PackageTable packages={packages} queue={CARD_QUEUE_BY_POSITION.REVIEWER} />
  }

  return (
    <>
      <PageHeader title={`${name}님의 업무 현황`} meta="승인자" />
      <section className={styles.section} aria-labelledby="reviewer-home-cards">
        <div className={styles.sectionHead}>
          <h2 id="reviewer-home-cards">Card 현황</h2>
          <SegmentedControl items={tabItems} value={tab} onChange={setTab} ariaLabel="Card 상태" />
        </div>
        <CardGrid
          cards={cardsQuery.data && filterByStatus(cardsQuery.data, tab)}
          isLoading={cardsQuery.isLoading}
          error={cardsQuery.error}
          onRetry={cardsQuery.refetch}
        />
      </section>
      <section className={styles.section} aria-labelledby="reviewer-home-packages">
        <div className={styles.sectionHead}>
          <h2 id="reviewer-home-packages">내 Package</h2>
          <Link className={styles.more} to={ROUTE_PATHS.packages}>
            전체 보기
          </Link>
        </div>
        {renderPackages()}
      </section>
    </>
  )
}
