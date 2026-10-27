import { confirmCard } from '@/features/card/api/cardApi.ts'
import { useInvalidateCardRelated } from '@/features/card/hooks/useInvalidateCardRelated.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function useConfirmCard() {
  const invalidateCardRelated = useInvalidateCardRelated()
  return useApiMutation({
    mutationFn: confirmCard,
    onSuccess: invalidateCardRelated,
  })
}
