import type { Position } from '@/features/auth/model/authTypes.ts'
import type { MemberSearchResult } from '@/features/member/model/memberTypes.ts'
import { apiRequest } from '@/shared/api/apiClient.ts'

type SearchMembersParams = { position: Position; keyword: string; limit?: number }

export function searchMembers(params: SearchMembersParams): Promise<MemberSearchResult[]> {
  const query = new URLSearchParams({ position: params.position })
  const keyword = params.keyword.trim()
  if (keyword !== '') {
    query.set('keyword', keyword)
  }
  if (params.limit !== undefined) {
    query.set('limit', String(params.limit))
  }
  return apiRequest<MemberSearchResult[]>(`/api/members?${query.toString()}`)
}
