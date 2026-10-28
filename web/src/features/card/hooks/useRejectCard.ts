import { rejectCard } from '@/features/card/api/cardApi.ts'
import { useInvalidateCardRelated } from '@/features/card/hooks/useInvalidateCardRelated.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

type RejectCardVariables = { id: string; reason: string }

export function useRejectCard() {
  const invalidateCardRelated = useInvalidateCardRelated()
  return useApiMutation({
    mutationFn: ({ id, reason }: RejectCardVariables) => rejectCard(id, reason),
    onSuccess: invalidateCardRelated,
  })
}
