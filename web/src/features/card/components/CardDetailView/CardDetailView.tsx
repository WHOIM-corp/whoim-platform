import { useState } from 'react'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { CardActions } from '@/features/card/components/CardActions/CardActions.tsx'
import { CardSidePanel } from '@/features/card/components/CardSidePanel/CardSidePanel.tsx'
import { TraceCard } from '@/features/card/components/TraceCard/TraceCard.tsx'
import { useCard } from '@/features/card/hooks/useCard.ts'
import { useCardActions } from '@/features/card/hooks/useCardActions.ts'
import type { TraceLetter } from '@/features/card/model/traceGroups.ts'
import { QueryErrorView } from '@/features/system/components/QueryErrorView/QueryErrorView.tsx'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import styles from '@/features/card/components/CardDetailView/CardDetailView.module.scss'

type CardDetailViewProps = {
  cardId: string
}

// 다른 Card로 이동하면 key로 다시 마운트해 동작 오류·선택 항목이 따라오지 않게 한다.
export function CardDetailView({ cardId }: CardDetailViewProps) {
  const position = useAuthStore((state) => state.member?.position ?? null)
  const cardQuery = useCard(cardId)
  const actions = useCardActions(cardId)
  const [activeGroup, setActiveGroup] = useState<TraceLetter>('T')

  if (cardQuery.isLoading) {
    return <CardSkeletonGrid count={1} />
  }
  if (cardQuery.error !== null) {
    return <QueryErrorView error={cardQuery.error} title="Card를 불러오지 못했습니다" onRetry={cardQuery.refetch} />
  }
  const card = cardQuery.data
  if (card === undefined) {
    return null
  }

  return (
    <div className={styles.stage}>
      <div className={styles.cardColumn}>
        {/* 카드 아래는 창 높이에서 화면 밖이라, 동작 실패는 카드 위에 보여준다. */}
        {actions.error && (
          <div className={styles.notice}>
            <ErrorNotice title="처리하지 못했습니다" message={actions.error.message} compact />
          </div>
        )}
        <TraceCard card={card} activeGroup={activeGroup} onSelectGroup={setActiveGroup} />
        <CardActions card={card} actions={actions} />
      </div>
      <CardSidePanel card={card} activeGroup={activeGroup} withHistory={position !== 'SUCCESSOR'} />
    </div>
  )
}
