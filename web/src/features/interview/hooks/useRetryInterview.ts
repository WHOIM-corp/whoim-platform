import { useQueryClient } from '@tanstack/react-query'
import { retryInterview } from '@/features/interview/api/interviewApi.ts'
import { interviewKeys } from '@/features/interview/model/interviewKeys.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function useRetryInterview() {
  const queryClient = useQueryClient()
  return useApiMutation({
    mutationFn: retryInterview,
    onSuccess: (interview) => {
      queryClient.setQueryData(interviewKeys.detail(interview.id), interview)
    },
  })
}
