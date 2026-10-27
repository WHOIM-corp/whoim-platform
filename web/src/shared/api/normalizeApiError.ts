import { ApiError, toApiError } from '@/shared/api/apiError.ts'

export function normalizeApiError(error: unknown): ApiError | null {
  if (error === null || error === undefined) {
    return null
  }
  return error instanceof ApiError ? error : toApiError(0, undefined)
}
