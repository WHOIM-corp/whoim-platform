import { CardGrid } from '@/features/card/components/CardGrid/CardGrid.tsx'
import { useCards } from '@/features/card/hooks/useCards.ts'
import type { CardQueue } from '@/features/card/model/cardQueue.ts'
import { PageHeader } from '@/shared/components/PageHeader/PageHeader.tsx'

type CardQueueViewProps = {
  queue: CardQueue
}

export function CardQueueView({ queue }: CardQueueViewProps) {
  const cardsQuery = useCards(queue.status)
  return (
    <>
      <PageHeader title={queue.label} meta={cardsQuery.data && `${cardsQuery.data.length}장`} />
      <CardGrid
        cards={cardsQuery.data}
        isLoading={cardsQuery.isLoading}
        error={cardsQuery.error}
        onRetry={cardsQuery.refetch}
        emptyTitle={`${queue.label} Card가 없습니다`}
      />
    </>
  )
}
