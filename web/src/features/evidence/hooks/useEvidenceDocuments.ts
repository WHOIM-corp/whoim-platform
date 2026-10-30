import { listEvidenceDocuments } from '@/features/evidence/api/evidenceApi.ts'
import { evidenceKeys } from '@/features/evidence/model/evidenceKeys.ts'
import { useApiQuery } from '@/shared/api/useApiQuery.ts'

export function useEvidenceDocuments(packageId: string) {
  return useApiQuery({
    queryKey: evidenceKeys.list(packageId),
    queryFn: () => listEvidenceDocuments(packageId),
  })
}
