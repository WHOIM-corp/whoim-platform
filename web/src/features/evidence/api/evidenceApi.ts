import type { DocumentType, EvidenceDocument } from '@/features/evidence/model/evidenceTypes.ts'
import { apiRequest } from '@/shared/api/apiClient.ts'

export function listEvidenceDocuments(packageId: string): Promise<EvidenceDocument[]> {
  const query = new URLSearchParams({ handoverPackageId: packageId })
  return apiRequest<EvidenceDocument[]>(`/api/evidence-documents?${query.toString()}`)
}

export function uploadEvidenceDocument(params: {
  packageId: string
  documentType: DocumentType
  file: File
}): Promise<EvidenceDocument> {
  const formData = new FormData()
  formData.append('handoverPackageId', params.packageId)
  formData.append('documentType', params.documentType)
  formData.append('file', params.file)
  return apiRequest<EvidenceDocument>('/api/evidence-documents', { method: 'POST', body: formData })
}
