import { listGaps } from '@/features/gap/api/gapApi.ts'
import { gapKeys } from '@/features/gap/model/gapKeys.ts'
import { useApiQuery } from '@/shared/api/useApiQuery.ts'

export function useGaps(packageId: string) {
  return useApiQuery({ queryKey: gapKeys.list(packageId), queryFn: () => listGaps(packageId) })
}
