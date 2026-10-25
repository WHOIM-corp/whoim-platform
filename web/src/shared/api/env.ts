const rawApiBaseUrl: unknown = import.meta.env.VITE_API_BASE_URL

if (typeof rawApiBaseUrl !== 'string' || rawApiBaseUrl.trim() === '') {
  throw new Error('환경 변수 VITE_API_BASE_URL이 설정되지 않았습니다.')
}

export const apiBaseUrl: string = rawApiBaseUrl.trim().replace(/\/+$/, '')
