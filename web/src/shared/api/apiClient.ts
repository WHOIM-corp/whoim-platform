import { ApiError, toApiError } from '@/shared/api/apiError.ts'
import { apiBaseUrl } from '@/shared/api/env.ts'

type ApiClientConfig = {
  getAccessToken: () => string | null
  onUnauthorized: () => void
}

type ApiRequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  authenticated?: boolean
}

let config: ApiClientConfig = {
  getAccessToken: () => null,
  onUnauthorized: () => {},
}

export function configureApiClient(nextConfig: ApiClientConfig): void {
  config = nextConfig
}

async function readJsonBody(response: Response): Promise<unknown> {
  const text = await response.text().catch(() => '')
  if (text === '') {
    return undefined
  }
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { method = 'GET', body, authenticated = true } = options
  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const accessToken = authenticated ? config.getAccessToken() : null
  if (accessToken !== null) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  let response: Response
  try {
    response = await fetch(apiBaseUrl + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(
      0,
      'NETWORK_ERROR',
      '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    )
  }

  const responseBody = await readJsonBody(response)
  if (!response.ok) {
    if (authenticated && response.status === 401) {
      config.onUnauthorized()
    }
    throw toApiError(response.status, responseBody)
  }
  // 204 또는 빈 본문은 undefined이며, 호출하는 쪽이 T를 undefined 허용 타입으로 지정한다.
  return responseBody as T
}
