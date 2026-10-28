import { getPackage } from '@/features/handover/api/handoverApi.ts'
import { handoverKeys } from '@/features/handover/model/handoverKeys.ts'
import { useApiQuery } from '@/shared/api/useApiQuery.ts'

export function useHandoverPackage(id: string) {
  return useApiQuery({ queryKey: handoverKeys.detail(id), queryFn: () => getPackage(id) })
}
