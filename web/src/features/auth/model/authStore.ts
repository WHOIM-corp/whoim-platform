import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import { isPersistedAuth } from '@/features/auth/model/authGuards.ts'
import type { AuthMember, LoginResponse } from '@/features/auth/model/authTypes.ts'
import { STORAGE_KEYS } from '@/features/auth/model/storageKeys.ts'
import { safeStorage } from '@/shared/lib/safeStorage.ts'

type AuthState = {
  accessToken: string | null
  member: AuthMember | null
  // 저장하지 않는 값: 세션 만료로 로그아웃된 직후에만 true라 새로고침하면 사라진다.
  sessionExpired: boolean
  signIn: (response: LoginResponse) => void
  signOut: () => void
  expireSession: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      member: null,
      sessionExpired: false,
      signIn: (response) =>
        set({ accessToken: response.accessToken, member: response.member, sessionExpired: false }),
      signOut: () => set({ accessToken: null, member: null, sessionExpired: false }),
      // 로그인 상태일 때만 만료 처리해, 로그아웃 뒤 늦게 도착한 401이 안내를 만들지 못하게 한다.
      expireSession: () =>
        set((state) =>
          state.member === null ? state : { accessToken: null, member: null, sessionExpired: true },
        ),
    }),
    {
      name: STORAGE_KEYS.auth,
      storage: createJSONStorage(() => safeStorage),
      partialize: (state) => ({ accessToken: state.accessToken, member: state.member }),
      // persist는 병합 결과로 상태를 교체하므로, 모양이 다른 저장값은 여기서 걸러 비로그인으로 시작한다.
      merge: (persistedState, currentState) => {
        if (!isPersistedAuth(persistedState)) {
          return currentState
        }
        return {
          ...currentState,
          accessToken: persistedState.accessToken,
          member: persistedState.member,
        }
      },
    },
  ),
)
