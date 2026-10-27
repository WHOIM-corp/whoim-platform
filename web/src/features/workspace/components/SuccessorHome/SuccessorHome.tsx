import { Layers, MessagesSquare } from 'lucide-react'
import { Link } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { CardGrid } from '@/features/card/components/CardGrid/CardGrid.tsx'
import { useCards } from '@/features/card/hooks/useCards.ts'
import { latestCards } from '@/features/card/model/latestCards.ts'
import { PackageTable } from '@/features/handover/components/PackageTable/PackageTable.tsx'
import { useHandoverPackages } from '@/features/handover/hooks/useHandoverPackages.ts'
import { buttonClassName } from '@/shared/components/Button/buttonClassName.ts'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'
import { StateView } from '@/shared/components/StateView/StateView.tsx'
import styles from '@/features/workspace/components/SuccessorHome/SuccessorHome.module.scss'

type SuccessorHomeProps = {
  name: string
}

// 후임자에게 두 목록은 서버가 이미 공개된 Package의 승인된 Card로만 좁혀 보낸다.
export function SuccessorHome({ name }: SuccessorHomeProps) {
  const cardsQuery = useCards()
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
      return <StateView icon={Layers} title="아직 공개된 Package가 없습니다" message="공개되면 여기에 나타납니다." />
    }
    return <PackageTable packages={packages} />
  }

  return (
    <>
      <PageHeader title={`${name}님의 업무 현황`} meta="후임자" />
      <section className={styles.consult} aria-labelledby="successor-home-consult">
        <span className={styles.icon}>
          <MessagesSquare size={20} aria-hidden="true" />
        </span>
        <div className={styles.copy}>
          <h2 id="successor-home-consult">상담</h2>
          <p>
            지금 마주한 상황을 적으면, 공개된 Package의 승인된 Card에서 가까운 전임자의 판단과
            근거를 찾아 보여줍니다. 입력한 내용은 저장되지 않습니다.
          </p>
        </div>
        <Link className={buttonClassName('primary')} to={ROUTE_PATHS.consult}>
          상담 시작
        </Link>
      </section>
      <section className={styles.section} aria-labelledby="successor-home-packages">
        <div className={styles.sectionHead}>
          <h2 id="successor-home-packages">공개된 Package</h2>
          <Link className={styles.more} to={ROUTE_PATHS.packages}>
            전체 보기
          </Link>
        </div>
        {renderPackages()}
      </section>
      <section className={styles.section} aria-labelledby="successor-home-cards">
        <div className={styles.sectionHead}>
          <h2 id="successor-home-cards">최근 공개된 Card</h2>
        </div>
        <CardGrid
          cards={cardsQuery.data && latestCards(cardsQuery.data)}
          isLoading={cardsQuery.isLoading}
          error={cardsQuery.error}
          onRetry={cardsQuery.refetch}
          emptyTitle="아직 공개된 Card가 없습니다"
          emptyMessage="공개된 Package의 승인된 Card가 여기에 나타납니다."
        />
      </section>
    </>
  )
}
