import { useQueryClient } from '@tanstack/react-query'
import { gapKeys } from '@/features/gap/model/gapKeys.ts'
import { submitAnswer } from '@/features/interview/api/interviewApi.ts'
import { interviewKeys } from '@/features/interview/model/interviewKeys.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

type SubmitAnswerVariables = { id: string; answer: string }

export function useSubmitAnswer() {
  const queryClient = useQueryClient()
  return useApiMutation({
    mutationFn: ({ id, answer }: SubmitAnswerVariables) => submitAnswer(id, answer),
    onSuccess: (interview) => {
      queryClient.setQueryData(interviewKeys.detail(interview.id), interview)
      return queryClient.invalidateQueries({ queryKey: gapKeys.all })
    },
  })
}
