import { useQueryClient } from '@tanstack/react-query'
import { publishPackage } from '@/features/handover/api/handoverApi.ts'
import { handoverKeys } from '@/features/handover/model/handoverKeys.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function usePublishPackage() {
  const queryClient = useQueryClient()
  return useApiMutation({
    mutationFn: publishPackage,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: handoverKeys.all }),
  })
}
