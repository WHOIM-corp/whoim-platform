import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/shared/api/apiError.ts'

// 4xx(403·404 등)는 다시 보내도 결과가 같으므로 재시도하지 않는다.
function shouldRetry(failureCount: number, error: unknown): boolean {
  const isClientError = error instanceof ApiError && error.status >= 400 && error.status < 500
  return !isClientError && failureCount < 1
}

export const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: shouldRetry }, mutations: { retry: false } },
})
