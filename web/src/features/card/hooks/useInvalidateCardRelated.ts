import { useQueryClient } from '@tanstack/react-query'
import { cardKeys } from '@/features/card/model/cardKeys.ts'
import { gapKeys } from '@/features/gap/model/gapKeys.ts'
import { handoverKeys } from '@/features/handover/model/handoverKeys.ts'

// Card 상태가 바뀌면 Card 목록·상세, Package 진행률, Gap 진행 상태가 함께 바뀐다.
export function useInvalidateCardRelated(): () => Promise<unknown> {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: cardKeys.all }),
      queryClient.invalidateQueries({ queryKey: handoverKeys.all }),
      queryClient.invalidateQueries({ queryKey: gapKeys.all }),
    ])
}
