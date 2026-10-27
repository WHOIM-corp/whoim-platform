import type {
  ConsultAnswer,
  ConsultQuestions,
  ConsultSuggestions,
} from '@/features/consult/model/consultTypes.ts'
import { apiRequest } from '@/shared/api/apiClient.ts'

export function askConsultQuestions(situation: string): Promise<ConsultQuestions> {
  return apiRequest<ConsultQuestions>('/api/consultations/questions', {
    method: 'POST',
    body: { situation },
  })
}

export function requestConsultSuggestions(
  situation: string,
  answers: ConsultAnswer[],
): Promise<ConsultSuggestions> {
  return apiRequest<ConsultSuggestions>('/api/consultations/suggestions', {
    method: 'POST',
    body: { situation, answers },
  })
}
