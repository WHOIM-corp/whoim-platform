import { useQueryClient } from '@tanstack/react-query'
import { analyzeGaps } from '@/features/gap/api/gapApi.ts'
import { gapKeys } from '@/features/gap/model/gapKeys.ts'
import { handoverKeys } from '@/features/handover/model/handoverKeys.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function useAnalyzeGaps() {
  const queryClient = useQueryClient()
  return useApiMutation({
    mutationFn: analyzeGaps,
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: gapKeys.all }),
        queryClient.invalidateQueries({ queryKey: handoverKeys.all }),
      ]),
  })
}
