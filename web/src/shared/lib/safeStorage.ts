import type { StateStorage } from 'zustand/middleware'

// 사생활 보호 모드 등에서 localStorage가 예외를 던져도 앱이 죽지 않게 모두 삼킨다.
export const safeStorage: StateStorage = {
  getItem: (name) => {
    try {
      return window.localStorage.getItem(name)
    } catch {
      return null
    }
  },
  setItem: (name, value) => {
    try {
      window.localStorage.setItem(name, value)
    } catch {
      // 저장 실패는 무시한다.
    }
  },
  removeItem: (name) => {
    try {
      window.localStorage.removeItem(name)
    } catch {
      // 삭제 실패는 무시한다.
    }
  },
}
