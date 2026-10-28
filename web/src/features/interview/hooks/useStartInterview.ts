import { useQueryClient } from '@tanstack/react-query'
import { gapKeys } from '@/features/gap/model/gapKeys.ts'
import { startInterview } from '@/features/interview/api/interviewApi.ts'
import { interviewKeys } from '@/features/interview/model/interviewKeys.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function useStartInterview() {
  const queryClient = useQueryClient()
  return useApiMutation({
    mutationFn: startInterview,
    onSuccess: (interview) => {
      queryClient.setQueryData(interviewKeys.detail(interview.id), interview)
      return queryClient.invalidateQueries({ queryKey: gapKeys.all })
    },
  })
}
