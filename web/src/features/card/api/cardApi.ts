import type { CardDetail, CardStatus, CardSummary } from '@/features/card/model/cardTypes.ts'
import { apiRequest } from '@/shared/api/apiClient.ts'

export function listCards(status?: CardStatus): Promise<CardSummary[]> {
  const query = new URLSearchParams()
  if (status !== undefined) {
    query.set('status', status)
  }
  const queryString = query.toString()
  return apiRequest<CardSummary[]>(queryString === '' ? '/api/cards' : `/api/cards?${queryString}`)
}

export function getCard(id: string): Promise<CardDetail> {
  return apiRequest<CardDetail>(`/api/cards/${id}`)
}

export function confirmCard(id: string): Promise<CardDetail> {
  return apiRequest<CardDetail>(`/api/cards/${id}/confirm`, { method: 'POST' })
}

export function approveCard(id: string): Promise<CardDetail> {
  return apiRequest<CardDetail>(`/api/cards/${id}/approve`, { method: 'POST' })
}

export function rejectCard(id: string, reason: string): Promise<CardDetail> {
  return apiRequest<CardDetail>(`/api/cards/${id}/reject`, { method: 'POST', body: { reason } })
}
