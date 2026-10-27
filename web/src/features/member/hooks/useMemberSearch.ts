import { keepPreviousData } from '@tanstack/react-query'
import type { Position } from '@/features/auth/model/authTypes.ts'
import { searchMembers } from '@/features/member/api/memberApi.ts'
import { memberKeys } from '@/features/member/model/memberKeys.ts'
import type { MemberSearchResult } from '@/features/member/model/memberTypes.ts'
import type { ApiError } from '@/shared/api/apiError.ts'
import { useApiQuery } from '@/shared/api/useApiQuery.ts'
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue.ts'

const KEYWORD_DEBOUNCE_MILLISECONDS = 250

type UseMemberSearchResult = {
  members: MemberSearchResult[]
  isLoading: boolean
  error: ApiError | null
  // members가 속한 검색어. 입력 중인 검색어와 다를 수 있다.
  settledKeyword: string
  // 입력이 아직 반영되지 않았거나 이전 검색어의 결과를 보여주는 중이다.
  isStale: boolean
}

// enabled는 검색창에 포커스가 있을 때만 true로 넘긴다.
export function useMemberSearch(
  position: Position,
  keyword: string,
  enabled: boolean,
): UseMemberSearchResult {
  const trimmedKeyword = keyword.trim()
  const debouncedKeyword = useDebouncedValue(trimmedKeyword, KEYWORD_DEBOUNCE_MILLISECONDS)

  const query = useApiQuery({
    queryKey: memberKeys.search(position, debouncedKeyword),
    queryFn: () => searchMembers({ position, keyword: debouncedKeyword }),
    enabled,
    placeholderData: keepPreviousData,
  })

  return {
    members: query.data ?? [],
    isLoading: query.isLoading,
    error: query.error,
    settledKeyword: debouncedKeyword,
    isStale: trimmedKeyword !== debouncedKeyword || query.isPlaceholderData,
  }
}
