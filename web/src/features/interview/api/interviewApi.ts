import type { CreatedCard } from '@/features/card/model/cardTypes.ts'
import type { Interview } from '@/features/interview/model/interviewTypes.ts'
import { apiRequest } from '@/shared/api/apiClient.ts'

export function startInterview(gapId: string): Promise<Interview> {
  return apiRequest<Interview>('/api/interviews', { method: 'POST', body: { gapId } })
}

export function getInterview(id: string): Promise<Interview> {
  return apiRequest<Interview>(`/api/interviews/${id}`)
}

export function submitAnswer(id: string, answer: string): Promise<Interview> {
  return apiRequest<Interview>(`/api/interviews/${id}/answers`, { method: 'POST', body: { answer } })
}

export function retryInterview(id: string): Promise<Interview> {
  return apiRequest<Interview>(`/api/interviews/${id}/retry`, { method: 'POST' })
}

export function createCardFromInterview(interviewId: string): Promise<CreatedCard> {
  return apiRequest<CreatedCard>(`/api/interviews/${interviewId}/card`, { method: 'POST' })
}
