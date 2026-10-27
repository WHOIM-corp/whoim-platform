import { useQueryClient } from '@tanstack/react-query'
import { createPackage } from '@/features/handover/api/handoverApi.ts'
import { handoverKeys } from '@/features/handover/model/handoverKeys.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function useCreatePackage() {
  const queryClient = useQueryClient()
  return useApiMutation({
    mutationFn: createPackage,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: handoverKeys.all }),
  })
}
