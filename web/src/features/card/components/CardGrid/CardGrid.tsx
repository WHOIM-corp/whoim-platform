import { Inbox } from 'lucide-react'
import { generatePath, useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { MiniCard } from '@/features/card/components/MiniCard/MiniCard.tsx'
import type { CardSummary } from '@/features/card/model/cardTypes.ts'
import type { HandoverCardSummary } from '@/features/handover/model/handoverTypes.ts'
import type { ApiError } from '@/shared/api/apiError.ts'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { StateView } from '@/shared/components/StateView/StateView.tsx'
import styles from '@/features/card/components/CardGrid/CardGrid.module.scss'

type CardGridItem = CardSummary | HandoverCardSummary

type CardGridProps = {
  cards: CardGridItem[] | undefined
  isLoading?: boolean
  error?: ApiError | null
  onRetry?: () => void
  emptyTitle?: string
  emptyMessage?: string
  // 열 수 없는 Card면 그 이유를 돌려준다. 이런 Card는 상세를 조회하지도, 이동하지도 않는다.
  closedReasonOf?: (card: CardGridItem) => string | undefined
}

export function CardGrid({
  cards,
  isLoading = false,
  error = null,
  onRetry,
  emptyTitle = '표시할 Card가 없습니다',
  emptyMessage = '조건에 맞는 Card가 생기면 여기에 나타납니다.',
  closedReasonOf,
}: CardGridProps) {
  const navigate = useNavigate()

  if (isLoading) {
    return <CardSkeletonGrid />
  }
  if (error !== null) {
    return (
      <ErrorNotice
        title="Card를 불러오지 못했습니다"
        message={error.message}
        actionLabel={onRetry ? '다시 시도' : undefined}
        onAction={onRetry}
      />
    )
  }
  if (cards === undefined || cards.length === 0) {
    return <StateView icon={Inbox} title={emptyTitle} message={emptyMessage} />
  }
  return (
    <div className={styles.grid}>
      {cards.map((card) => {
        const closedReason = closedReasonOf?.(card)
        return (
          <MiniCard
            key={card.id}
            summary={card}
            disabledReason={closedReason}
            onOpen={
              closedReason === undefined
                ? () => navigate(generatePath(ROUTE_PATHS.cardDetail, { cardId: card.id }))
                : undefined
            }
          />
        )
      })}
    </div>
  )
}
