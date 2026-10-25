import { queryClient } from '@/app/queryClient.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'

// 로그인 계정이 바뀌거나 로그아웃·만료되면 이전 계정의 쿼리 캐시를 비운다.
export function syncQueryCacheWithAuth(): void {
  useAuthStore.subscribe((state, previousState) => {
    if (state.member?.memberId !== previousState.member?.memberId) {
      queryClient.clear()
    }
  })
}
