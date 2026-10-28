import type { CreatePackageRequest, HandoverPackage } from '@/features/handover/model/handoverTypes.ts'
import { apiRequest } from '@/shared/api/apiClient.ts'

export function listPackages(): Promise<HandoverPackage[]> {
  return apiRequest<HandoverPackage[]>('/api/handover-packages')
}

export function getPackage(id: string): Promise<HandoverPackage> {
  return apiRequest<HandoverPackage>(`/api/handover-packages/${id}`)
}

export function createPackage(request: CreatePackageRequest): Promise<HandoverPackage> {
  return apiRequest<HandoverPackage>('/api/handover-packages', { method: 'POST', body: request })
}

export function publishPackage(id: string): Promise<HandoverPackage> {
  return apiRequest<HandoverPackage>(`/api/handover-packages/${id}/publish`, { method: 'POST' })
}
