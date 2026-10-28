import { CardGrid } from '@/features/card/components/CardGrid/CardGrid.tsx'
import { useCards } from '@/features/card/hooks/useCards.ts'
import { CARD_QUEUE_BY_POSITION } from '@/features/card/model/cardQueue.ts'
import { PackageTable } from '@/features/handover/components/PackageTable/PackageTable.tsx'
import { useHandoverPackages } from '@/features/handover/hooks/useHandoverPackages.ts'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'
import styles from '@/features/workspace/components/PredecessorHome/PredecessorHome.module.scss'

type PredecessorHomeProps = {
  name: string
}

// 전임자는 내 Package의 상태와 Card 수, 그리고 확인을 기다리는 Card를 본다.
export function PredecessorHome({ name }: PredecessorHomeProps) {
  const draftCardsQuery = useCards('DRAFT')
  const packagesQuery = useHandoverPackages()

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
      return <p className={styles.empty}>아직 배정된 Package가 없습니다.</p>
    }
    return <PackageTable packages={packages} queue={CARD_QUEUE_BY_POSITION.PREDECESSOR} />
  }

  return (
    <>
      <PageHeader title={`${name}님의 업무 현황`} meta="전임자" />
      <section className={styles.section} aria-labelledby="predecessor-home-packages">
        <div className={styles.sectionHead}>
          <h2 id="predecessor-home-packages">Package 진행</h2>
        </div>
        {renderPackages()}
      </section>
      <section className={styles.section} aria-labelledby="predecessor-home-cards">
        <div className={styles.sectionHead}>
          <h2 id="predecessor-home-cards">확인 대기 Card</h2>
        </div>
        <p className={styles.hint}>내용을 확인하고 &quot;작성 완료&quot;를 누르면 검수 대기로 넘어갑니다.</p>
        <CardGrid
          cards={draftCardsQuery.data}
          isLoading={draftCardsQuery.isLoading}
          error={draftCardsQuery.error}
          onRetry={draftCardsQuery.refetch}
          emptyTitle="확인 대기 Card가 없습니다"
          emptyMessage="인터뷰에서 Card가 만들어지면 여기에 나타납니다."
        />
      </section>
    </>
  )
}
