import { askConsultQuestions } from '@/features/consult/api/consultApi.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function useConsultQuestions() {
  return useApiMutation({ mutationFn: askConsultQuestions })
}
