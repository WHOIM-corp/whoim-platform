import { useQueryClient } from '@tanstack/react-query'
import { uploadEvidenceDocument } from '@/features/evidence/api/evidenceApi.ts'
import { evidenceKeys } from '@/features/evidence/model/evidenceKeys.ts'
import { useApiMutation } from '@/shared/api/useApiMutation.ts'

export function useUploadEvidenceDocument() {
  const queryClient = useQueryClient()
  return useApiMutation({
    mutationFn: uploadEvidenceDocument,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: evidenceKeys.all }),
  })
}
