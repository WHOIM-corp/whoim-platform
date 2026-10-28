import { createCardFromInterview } from '@/features/interview/api/interviewApi.ts'
import { useInvalidateCardRelated } from '@/features/card/hooks/useInvalidateCardRelated.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function useCreateCardFromInterview() {
  const invalidateCardRelated = useInvalidateCardRelated()
  return useApiMutation({
    mutationFn: createCardFromInterview,
    onSuccess: invalidateCardRelated,
  })
}
