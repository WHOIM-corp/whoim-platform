export type ApiErrorBody = { errorCode: string; errorMessage: string }

export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string

  constructor(status: number, errorCode: string, errorMessage: string) {
    super(errorMessage)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
  }
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  return (
    typeof value === 'object' &&
    value !== null &&
    'errorCode' in value &&
    'errorMessage' in value &&
    typeof value.errorCode === 'string' &&
    typeof value.errorMessage === 'string'
  )
}

export function toApiError(status: number, body: unknown): ApiError {
  if (isApiErrorBody(body)) {
    return new ApiError(status, body.errorCode, body.errorMessage)
  }
  return new ApiError(status, 'UNKNOWN_ERROR', '알 수 없는 오류가 발생했습니다.')
}
