import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { configureApiClient } from '@/shared/api/apiClient.ts'

export function configureApi(): void {
  configureApiClient({
    getAccessToken: () => useAuthStore.getState().accessToken,
    // 이동은 RequireAuth가 한 번만 한다(from 보존). 여기서는 상태만 바꾼다.
    onUnauthorized: () => useAuthStore.getState().expireSession(),
  })
}
