import { useQuery, type keepPreviousData, type QueryKey } from '@tanstack/react-query'
import type { ApiError } from '@/shared/api/apiError.ts'
import { normalizeApiError } from '@/shared/api/normalizeApiError.ts'

export type ApiQueryResult<Data> = {
  data: Data | undefined
  isLoading: boolean
  // placeholderData(이전 키의 데이터)를 보여주는 중이다.
  isPlaceholderData: boolean
  error: ApiError | null
  refetch: () => void
}

type ApiQueryConfig<Data> = {
  queryKey: QueryKey
  queryFn: () => Promise<Data>
  enabled?: boolean
  // 키가 바뀌는 동안 이전 데이터를 유지할 때 keepPreviousData를 넘긴다.
  placeholderData?: typeof keepPreviousData
}

export function useApiQuery<Data>(config: ApiQueryConfig<Data>): ApiQueryResult<Data> {
  const query = useQuery(config)
  return {
    data: query.data,
    isLoading: query.isLoading,
    isPlaceholderData: query.isPlaceholderData,
    error: normalizeApiError(query.error),
    refetch: () => void query.refetch(),
  }
}
