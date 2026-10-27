import type { Gap } from '@/features/gap/model/gapTypes.ts'
import { apiRequest } from '@/shared/api/apiClient.ts'

export function listGaps(packageId: string): Promise<Gap[]> {
  const query = new URLSearchParams({ handoverPackageId: packageId })
  return apiRequest<Gap[]>(`/api/gaps?${query.toString()}`)
}

export function analyzeGaps(packageId: string): Promise<Gap[]> {
  return apiRequest<Gap[]>(`/api/handover-packages/${packageId}/gap-analysis`, { method: 'POST' })
}
