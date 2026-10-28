import { listPackages } from '@/features/handover/api/handoverApi.ts'
import { handoverKeys } from '@/features/handover/model/handoverKeys.ts'
import { useApiQuery } from '@/shared/api/useApiQuery.ts'

export function useHandoverPackages() {
  return useApiQuery({ queryKey: handoverKeys.list(), queryFn: listPackages })
}
