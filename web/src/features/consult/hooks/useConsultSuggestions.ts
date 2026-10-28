import { requestConsultSuggestions } from '@/features/consult/api/consultApi.ts'
import type { ConsultAnswer } from '@/features/consult/model/consultTypes.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

type ConsultSuggestionsVariables = { situation: string; answers: ConsultAnswer[] }

export function useConsultSuggestions() {
  return useApiMutation({
    mutationFn: ({ situation, answers }: ConsultSuggestionsVariables) =>
      requestConsultSuggestions(situation, answers),
  })
}
